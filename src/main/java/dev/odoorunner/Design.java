package dev.odoorunner;

import javax.swing.*;
import java.awt.*;

/** Native theme colors for text and outlines only; cards and badges never paint fills. */
final class Design {
    private static Color theme(String key,Color fallback){Color color=UIManager.getColor(key);return color==null?fallback:color;}
    static final Color BORDER=theme("Component.borderColor",Color.GRAY),TEXT=theme("Label.foreground",Color.GRAY),MUTED=theme("Label.disabledForeground",Color.GRAY),BLUE=new Color(0,185,162),DANGER=new Color(227,79,80);
    static class Card extends JPanel {
        Color outline=BORDER;final int radius;
        Card(LayoutManager layout,int radius){super(layout);this.radius=radius;setOpaque(false);}
        protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(outline);g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,radius,radius);g.dispose();super.paintComponent(graphics);}
    }
    static JLabel label(String value,int size,boolean bold){JLabel label=new JLabel(value);label.setFont(label.getFont().deriveFont(bold?Font.BOLD:Font.PLAIN,(float)size));label.setForeground(bold?TEXT:MUTED);return label;}
    static void primary(JButton button){button.putClientProperty("odoo.primary",true);button.setContentAreaFilled(false);button.setOpaque(false);button.setForeground(BLUE);RoundedNativeBorder.install(button);}
    static JComponent badge(Icon icon){return new JComponent(){public Dimension getPreferredSize(){return new Dimension(48,48);}protected void paintComponent(Graphics graphics){Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(BORDER);g.drawOval(1,1,46,46);g.translate(12,12);g.scale(1.5,1.5);icon.paintIcon(this,g,0,0);g.dispose();}};}
}
