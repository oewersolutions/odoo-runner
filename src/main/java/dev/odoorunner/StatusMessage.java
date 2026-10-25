package dev.odoorunner;

import javax.swing.*;
import java.awt.*;

/** Wrapping, foreground-only status message; details can be kept in its tooltip. */
final class StatusMessage extends JPanel {
    private final JLabel icon=new JLabel();private final JTextArea text=new JTextArea();
    StatusMessage(){super(new BorderLayout(8,0));setOpaque(false);setBorder(BorderFactory.createEmptyBorder(6,8,6,8));text.setOpaque(false);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setFont(UIManager.getFont("Label.font"));add(icon,BorderLayout.WEST);add(text);setVisible(false);}
    void setText(String value){message("Info",value);}
    void message(String type,String value){Color color=type.equals("Success")?new Color(45,160,100):type.equals("Error")?Design.DANGER:type.equals("Warning")?new Color(195,145,35):Design.MUTED;icon.setIcon(UiIcons.dot(color));text.setForeground(color);text.setText(type+" · "+value);setVisible(value!=null&&!value.isBlank());revalidate();}
}
