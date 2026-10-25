package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.ArrayDeque;

/** Bounded, time-scaled foreground-only chart; missing samples break the trace. */
final class PerformanceGraph extends JPanel {
    private record Point(long time,Double value){}
    private final ArrayDeque<Point> points=new ArrayDeque<>();
    private final String title,unit;
    private final double minimumScale;
    private String value="Waiting for samples…";
    PerformanceGraph(String title,String unit,double minimumScale){this.title=title;this.unit=unit;this.minimumScale=minimumScale;setPreferredSize(new Dimension(360,180));setMinimumSize(new Dimension(180,150));}
    void clear(){points.clear();value="Waiting for samples…";repaint();}
    void sample(Double sample,String label){points.addLast(new Point(System.currentTimeMillis(),sample));while(points.size()>120)points.removeFirst();value=label;repaint();}
    protected void paintComponent(Graphics graphics){
        super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        int left=52,right=getWidth()-16,top=54,bottom=getHeight()-28;
        g.setFont(getFont().deriveFont(Font.BOLD,13f));g.setColor(Design.TEXT);g.drawString(title,12,20);g.setFont(getFont().deriveFont(12f));g.setColor(Design.BLUE);g.drawString(value,12,39);
        double max=minimumScale;for(Point point:points)if(point.value()!=null)max=Math.max(max,point.value()*1.1);if(max<=0)max=1;
        g.setColor(Design.BORDER);for(int n=0;n<3;n++){int y=top+(bottom-top)*n/2;g.drawLine(left,y,right,y);g.setColor(Design.MUTED);g.drawString(String.format(java.util.Locale.ROOT,"%.0f",max*(2-n)/2),4,y+4);g.setColor(Design.BORDER);}
        long now=System.currentTimeMillis(),start=now-240000;Path2D line=new Path2D.Double();boolean drawing=false;
        for(Point point:points){if(point.time()<start)continue;if(point.value()==null){drawing=false;continue;}double x=left+(right-left)*(point.time()-start)/240000.0,y=bottom-(bottom-top)*point.value()/max;if(drawing)line.lineTo(x,y);else{line.moveTo(x,y);drawing=true;}}
        g.setColor(Design.BLUE);g.setStroke(new BasicStroke(1.8f));g.draw(line);g.setColor(Design.MUTED);g.drawString("4 minutes ago",left,getHeight()-8);String nowLabel="Now · "+unit;g.drawString(nowLabel,Math.max(left,right-g.getFontMetrics().stringWidth(nowLabel)),getHeight()-8);g.dispose();
    }
}
