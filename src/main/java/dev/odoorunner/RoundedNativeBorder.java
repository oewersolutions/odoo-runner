package dev.odoorunner;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** Round the existing native border without drawing an additional outline. */
final class RoundedNativeBorder extends AbstractBorder {
    private final Border nativeBorder;
    private RoundedNativeBorder(Border nativeBorder){this.nativeBorder=nativeBorder;}
    static void install(JComponent component){if(!(component.getBorder() instanceof RoundedNativeBorder))component.setBorder(new RoundedNativeBorder(component.getBorder()));component.putClientProperty("JComponent.roundRect",Boolean.TRUE);}
    @Override public Insets getBorderInsets(Component component){return nativeBorder==null?new Insets(4,8,4,8):nativeBorder.getBorderInsets(component);}
    @Override public Insets getBorderInsets(Component component,Insets target){Insets source=getBorderInsets(component);target.set(source.top,source.left,source.bottom,source.right);return target;}
    @Override public void paintBorder(Component component,Graphics graphics,int x,int y,int width,int height){if(nativeBorder==null)return;Graphics2D g=(Graphics2D)graphics.create();g.clip(new RoundRectangle2D.Double(x,y,width,height,10,10));nativeBorder.paintBorder(component,g,x,y,width,height);g.dispose();}
}
