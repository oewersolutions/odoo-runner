"""Single-request PostgreSQL bridge. Requests/secrets arrive only through stdin."""
import json
import sys


def run(request):
    import psycopg2
    from psycopg2 import sql

    settings = {key: value for key, value in request['connection'].items()
                if value and (key == 'password' or str(value).lower() not in ('false', 'none'))}
    settings.update(dbname=request['database'], connect_timeout=8,
                    application_name='Odoo Runner performance monitor' if request['action'] == 'metrics' else 'Odoo Runner database browser',
                    options='-c statement_timeout=12000 -c lock_timeout=3000')
    connection = psycopg2.connect(**settings)
    try:
        connection.set_session(readonly=request['action'] != 'update')
        with connection.cursor() as cursor:
            action = request['action']
            if action == 'metrics':
                cursor.execute("""
                    SELECT xact_commit,xact_rollback,blks_read,blks_hit,deadlocks,
                           stats_reset::text,pg_database_size(current_database()),
                           EXTRACT(EPOCH FROM clock_timestamp())::double precision
                    FROM pg_stat_database WHERE datname=current_database()
                """)
                r = cursor.fetchone()
                if r is None:
                    raise ValueError('No statistics are available for this database.')
                cursor.execute("""
                    SELECT count(*),count(*) FILTER (WHERE state='active'),
                           count(*) FILTER (WHERE state='idle'),
                           count(*) FILTER (WHERE state='idle in transaction'),
                           count(*) FILTER (WHERE wait_event_type='Lock'),
                           count(*) FILTER (WHERE state IS NULL)
                    FROM pg_stat_activity WHERE datname=current_database() AND backend_type='client backend'
                      AND pid<>pg_backend_pid()
                      AND application_name<>'Odoo Runner performance monitor'
                """)
                a = cursor.fetchone()
                return dict(zip(['commits','rollbacks','reads','hits','deadlocks','reset','size','time'], r),
                            **dict(zip(['connections','active','idle','idleTransaction','lockWaits','hiddenStates'], a)))
            if action == 'tables':
                cursor.execute('SELECT pg_database_size(current_database())')
                database_size = cursor.fetchone()[0]
                cursor.execute("""
                    SELECT n.nspname, c.relname, c.reltuples::bigint,
                           pg_table_size(c.oid) - CASE WHEN c.reltoastrelid=0 THEN 0 ELSE pg_total_relation_size(c.reltoastrelid) END,
                           CASE WHEN c.reltoastrelid=0 THEN 0 ELSE pg_total_relation_size(c.reltoastrelid) END,
                           pg_indexes_size(c.oid), pg_total_relation_size(c.oid), c.relkind,
                           COALESCE(obj_description(c.oid,'pg_class'),''), c.relispartition
                    FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                    WHERE c.relkind IN ('r','p','v','m','f','t')
                      AND NOT pg_is_other_temp_schema(n.oid)
                      AND (%s OR (n.nspname NOT IN ('pg_catalog','information_schema')
                           AND n.nspname NOT LIKE 'pg_toast%%' AND n.nspname NOT LIKE 'pg_temp_%%'))
                    ORDER BY n.nspname,c.relname
                """, (bool(request.get('system', False)),))
                tables = cursor.fetchall()
                models = {}
                # Optional Odoo metadata: missing tables/permissions must not hide SQL objects.
                cursor.execute('SAVEPOINT odoo_metadata')
                try:
                    cursor.execute('SELECT model,name::text FROM public.ir_model')
                    for model, label in cursor.fetchall():
                        try:
                            translated = json.loads(label)
                            if isinstance(translated, dict):
                                label = translated.get('en_US') or next(iter(translated.values()), model)
                        except (ValueError, TypeError):
                            pass
                        models.setdefault(model.replace('.', '_'), []).append({'model': model, 'label': label})
                except psycopg2.Error:
                    cursor.execute('ROLLBACK TO SAVEPOINT odoo_metadata')
                cursor.execute('RELEASE SAVEPOINT odoo_metadata')
                objects = []
                kinds = {'r': 'Table', 'p': 'Partitioned table', 'v': 'View', 'm': 'Materialized view', 'f': 'Foreign table', 't': 'TOAST table'}
                for r in tables:
                    matches = models.get(r[1], []) if r[0] == 'public' else []
                    objects.append({'schema': r[0], 'table': r[1], 'estimated': r[2],
                                    'data': r[3] or 0, 'toast': r[4] or 0, 'indexes': r[5] or 0, 'total': r[6] or 0,
                                    'kind': 'Partition' if r[9] else kinds[r[7]],
                                    'name': ' / '.join(str(m['label']) for m in matches) or r[8].split('\n')[0] or r[1],
                                    'models': ', '.join(m['model'] for m in matches)})
                return {'databaseBytes': database_size, 'tables': objects}

            schema, table = request['schema'], request['table']
            cursor.execute("""SELECT c.oid,c.relkind FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace
                              WHERE n.nspname=%s AND c.relname=%s AND c.relkind IN ('r','p','v','m','f','t')""", (schema, table))
            result = cursor.fetchone()
            if result is None:
                raise ValueError('Table no longer exists. Refresh the tables list.')
            oid, kind = result
            writable = kind in ('r', 'p') and not schema.startswith('pg_') and schema != 'information_schema'
            cursor.execute("""
                SELECT a.attname, format_type(a.atttypid,a.atttypmod),
                       EXISTS(SELECT 1 FROM pg_constraint k WHERE k.conrelid=a.attrelid AND k.contype='p' AND a.attnum=ANY(k.conkey)),
                       a.attgenerated='', a.attidentity='', a.attnotnull,
                       has_column_privilege(a.attrelid,a.attname,'UPDATE'),
                       a.atttypid NOT IN (17,26,27,28,29)
                FROM pg_attribute a WHERE a.attrelid=%s AND a.attnum>0 AND NOT a.attisdropped ORDER BY a.attnum
            """, (oid,))
            columns = [{'name': c[0], 'type': c[1], 'pk': c[2],
                        'editable': bool(writable and not c[2] and c[3] and c[4] and c[6] and c[7]), 'notNull': c[5]}
                       for c in cursor.fetchall()]
            if not columns:
                raise ValueError('This table has no visible columns to browse.')
            names = {c['name'] for c in columns}
            primary = [c['name'] for c in columns if c['pk']]
            relation = sql.Identifier(schema, table)
            if action == 'records':
                offset = max(0, min(int(request.get('offset', 0)), 10000000))
                needle = request.get('filter', '')
                column = request.get('column', '')
                if column and column not in names:
                    raise ValueError('Unknown filter column. Refresh the table.')
                params = []
                where = sql.SQL('')
                if needle:
                    search_names = [column] if column else [c['name'] for c in columns]
                    # strpos gives literal contains-search (no wildcard interpretation).
                    where = sql.SQL(' WHERE ') + sql.SQL(' OR ').join(
                        sql.SQL('strpos(lower({}::text),lower(%s))>0').format(sql.Identifier(name)) for name in search_names)
                    params.extend([needle] * len(search_names))
                fields = sql.SQL(',').join(sql.SQL('left({}::text,100000)').format(sql.Identifier(c['name'])) for c in columns)
                truncated = sql.SQL(' OR ').join(sql.SQL('length({}::text)>100000').format(sql.Identifier(c['name'])) for c in columns)
                order = (sql.SQL(' ORDER BY ') + sql.SQL(',').join(map(sql.Identifier, primary))) if primary else (sql.SQL(' ORDER BY tableoid,ctid') if writable else sql.SQL(''))
                version = sql.SQL('xmin::text') if writable else sql.SQL('NULL::text')
                query = sql.SQL('SELECT {},({}),{} FROM {}{}{} LIMIT 101 OFFSET %s').format(
                    version, truncated, fields, relation, where, order)
                params.append(offset)
                cursor.execute(query, params)
                result = cursor.fetchall()
                rows = [{'xmin': r[0], 'truncated': bool(r[1]), 'values': list(r[2:])} for r in result[:100]]
                return {'columns': columns, 'rows': rows, 'hasMore': len(result) > 100, 'offset': offset}
            if action != 'update':
                raise ValueError('Unsupported database action.')
            if not writable:
                raise ValueError('Views and foreign tables are read-only in this browser.')
            if not primary:
                raise ValueError('This table has no primary key and is read-only.')
            changes = request['changes']
            editable = {c['name'] for c in columns if c['editable']}
            if not changes or not set(changes).issubset(editable):
                raise ValueError('No editable changes, or a protected column was changed. Reload the records.')
            original = request['original']
            if len(original) != len(columns):
                raise ValueError('Table columns changed. Reload the records.')
            values = dict(zip([c['name'] for c in columns], original))
            conditions = [sql.SQL('{}::text = %s').format(sql.Identifier(key)) for key in primary]
            conditions.append(sql.SQL('xmin::text = %s'))
            query = sql.SQL('UPDATE {} SET {} WHERE {}').format(
                relation, sql.SQL(',').join(sql.SQL('{}=%s').format(sql.Identifier(key)) for key in changes),
                sql.SQL(' AND ').join(conditions))
            cursor.execute(query, list(changes.values()) + [values[key] for key in primary] + [request['xmin']])
            if cursor.rowcount != 1:
                raise ValueError('Record changed or was removed since loading. Reload before editing again; nothing was saved.')
            connection.commit()
            return {'saved': True}
    finally:
        connection.close()


try:
    response = run(json.load(sys.stdin))
    print(json.dumps({'ok': True, 'result': response}, ensure_ascii=True))
except ImportError:
    print(json.dumps({'ok': False, 'error': 'psycopg2 is missing from this project Python environment. Install the Odoo PostgreSQL dependencies in the project dependency screen.'}))
except Exception as exc:
    # PostgreSQL primary messages omit connection strings and failing-row/detail values.
    diagnostic = getattr(exc, 'diag', None)
    message = getattr(diagnostic, 'message_primary', None)
    if not message:
        message = str(exc) if isinstance(exc, ValueError) else 'Database connection/query failed. Check the saved host, port, user, password, SSL settings and permissions.'
    print(json.dumps({'ok': False, 'error': message}))
