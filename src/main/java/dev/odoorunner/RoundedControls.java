package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.function.Consumer;

/** Clip native painting itself to a five-pixel silhouette; no extra stroke or fill. */
final class RoundedControls {
    static void paint(Component component,Graphics graphics,Consumer<Graphics> nativePaint){Graphics2D copy=(Graphics2D)graphics.create();copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);copy.clip(new RoundRectangle2D.Double(0,0,component.getWidth(),component.getHeight(),10,10));nativePaint.accept(copy);copy.dispose();}
}
class RoundButton extends JButton {
    RoundButton(){super();}RoundButton(String text){super(text);}RoundButton(Icon icon){super(icon);}
    @Override public void paint(Graphics graphics){RoundedControls.paint(this,graphics,g->super.paint(g));}
}
class RoundTextField extends JTextField {
    RoundTextField(){super();}RoundTextField(String text){super(text);}RoundTextField(int columns){super(columns);}RoundTextField(String text,int columns){super(text,columns);}
    @Override public void paint(Graphics graphics){RoundedControls.paint(this,graphics,g->super.paint(g));}
}
class RoundPasswordField extends JPasswordField {
    @Override public void paint(Graphics graphics){RoundedControls.paint(this,graphics,g->super.paint(g));}
}
class RoundComboBox<E> extends JComboBox<E> {
    RoundComboBox(){super();}RoundComboBox(E[] values){super(values);}
    @Override public void paint(Graphics graphics){RoundedControls.paint(this,graphics,g->super.paint(g));}
}
