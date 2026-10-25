package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

final class UiIcons implements Icon {
    static final UiIcons DATABASE=new UiIcons("database"),CODE=new UiIcons("code"),TEST=new UiIcons("test"),GEAR=new UiIcons("gear"),FOLDER=new UiIcons("folder"),SAVE=new UiIcons("save"),PLAY=new UiIcons("play"),TERMINAL=new UiIcons("terminal"),GLOBE=new UiIcons("globe"),PLUS=new UiIcons("plus"),CUBE=new UiIcons("cube"),SERVER=new UiIcons("server"),EDIT=new UiIcons("edit"),DELETE=new UiIcons("delete"),STOP=new UiIcons("stop"),RESTART=new UiIcons("restart"),ISSUES=new UiIcons("issues"),LOG=new UiIcons("log"),PIP=new UiIcons("pip"),BACK=new UiIcons("back"),FORWARD=new UiIcons("forward");
    private final String kind;
    private UiIcons(String kind){this.kind=kind;}
    public int getIconWidth(){return 16;}public int getIconHeight(){return 16;}
    public void paintIcon(Component component,Graphics graphics,int x,int y){
        Graphics2D g=(Graphics2D)graphics.create();g.translate(x,y);g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);Color color=component.getForeground();g.setColor(color==null?Color.GRAY:color);g.setStroke(new BasicStroke(1.6f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
        switch(kind){
            case "database" -> {g.drawOval(2,1,12,4);g.drawLine(2,3,2,13);g.drawLine(14,3,14,13);g.drawArc(2,5,12,4,180,180);g.drawArc(2,9,12,5,180,180);}
            case "code" -> {g.drawLine(5,4,1,8);g.drawLine(1,8,5,12);g.drawLine(11,4,15,8);g.drawLine(15,8,11,12);g.drawLine(9,2,7,14);}
            case "test" -> {g.drawLine(5,1,11,1);g.drawLine(6,1,6,6);g.drawLine(10,1,10,6);g.drawLine(6,6,2,14);g.drawLine(10,6,14,14);g.drawLine(2,14,14,14);g.drawLine(5,10,11,10);}
            case "gear" -> {g.drawOval(4,4,8,8);g.drawOval(6,6,4,4);for(int n=0;n<8;n++){double angle=n*Math.PI/4;g.drawLine((int)(8+5*Math.cos(angle)),(int)(8+5*Math.sin(angle)),(int)(8+7*Math.cos(angle)),(int)(8+7*Math.sin(angle)));}}
            case "folder" -> {g.drawLine(1,4,1,13);g.drawLine(1,13,15,13);g.drawLine(15,13,15,4);g.drawLine(15,4,7,4);g.drawLine(7,4,5,2);g.drawLine(5,2,1,2);g.drawLine(1,2,1,4);}
            case "save" -> {g.drawRect(2,1,12,14);g.drawRect(5,1,6,5);g.drawRect(5,10,6,5);}
            case "play" -> g.drawPolygon(new int[]{3,13,3},new int[]{2,8,14},3);
            case "terminal" -> {g.drawLine(2,3,6,7);g.drawLine(6,7,2,11);g.drawLine(8,12,14,12);}
            case "globe" -> {g.drawOval(1,1,14,14);g.drawOval(5,1,6,14);g.drawLine(1,8,15,8);}
            case "plus" -> {g.drawLine(8,2,8,14);g.drawLine(2,8,14,8);}
            case "cube" -> {g.drawPolygon(new int[]{8,2,8,14},new int[]{1,4,7,4},4);g.drawLine(2,4,2,12);g.drawLine(14,4,14,12);g.drawLine(2,12,8,15);g.drawLine(14,12,8,15);g.drawLine(8,7,8,15);}
            case "server" -> {g.drawRoundRect(1,2,14,5,2,2);g.drawRoundRect(1,9,14,5,2,2);g.fillOval(3,4,1,1);g.fillOval(3,11,1,1);g.drawLine(8,4,12,4);g.drawLine(8,11,12,11);}
            case "edit" -> {g.drawLine(4,10,11,3);g.drawLine(6,12,13,5);g.drawLine(11,3,13,5);g.drawLine(4,10,3,13);g.drawLine(3,13,6,12);}
            case "delete" -> {g.drawLine(3,4,13,4);g.drawLine(6,2,10,2);g.drawLine(4,4,5,14);g.drawLine(5,14,11,14);g.drawLine(11,14,12,4);g.drawLine(7,7,7,11);g.drawLine(9,7,9,11);}
            case "stop" -> g.fillRoundRect(3,3,10,10,2,2);
            case "restart" -> {g.drawArc(3,3,10,10,45,290);g.drawLine(12,2,12,6);g.drawLine(8,6,12,6);}
            case "issues" -> {g.drawPolygon(new int[]{8,1,15},new int[]{1,14,14},3);g.drawLine(8,5,8,9);g.fillOval(7,11,2,2);}
            case "log" -> {g.drawRect(3,1,10,14);g.drawLine(5,5,11,5);g.drawLine(5,8,11,8);g.drawLine(5,11,9,11);}
            case "pip" -> {g.drawRect(2,5,12,9);g.drawLine(2,5,8,2);g.drawLine(8,2,14,5);g.drawLine(8,6,8,11);g.drawLine(6,9,8,11);g.drawLine(10,9,8,11);}
            case "back" -> {g.drawLine(11,3,5,8);g.drawLine(5,8,11,13);}
            case "forward" -> {g.drawLine(5,3,11,8);g.drawLine(11,8,5,13);}
        }g.dispose();
    }
    static JButton button(RunnerWindow.Panel owner,Icon icon,String label,Runnable action){
        JButton button=owner.button("",action);button.setIcon(icon);button.setToolTipText(label);button.getAccessibleContext().setAccessibleName(label);button.setPreferredSize(new Dimension(32,32));button.setContentAreaFilled(false);button.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));return button;
    }
    static void clickable(JComponent row,Runnable action){
        MouseAdapter listener=new MouseAdapter(){public void mouseClicked(MouseEvent e){if(SwingUtilities.isLeftMouseButton(e)&&e.getClickCount()==1)action.run();}};
        attach(row,listener);row.setFocusable(true);row.getInputMap().put(KeyStroke.getKeyStroke("ENTER"),"open");row.getInputMap().put(KeyStroke.getKeyStroke("SPACE"),"open");row.getActionMap().put("open",new AbstractAction(){public void actionPerformed(ActionEvent e){action.run();}});
    }
    private static void attach(Component component,MouseAdapter listener){if(component instanceof AbstractButton)return;component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));component.addMouseListener(listener);if(component instanceof Container c)for(Component child:c.getComponents())attach(child,listener);}
    static Icon dot(Color color){return new Icon(){public int getIconWidth(){return 10;}public int getIconHeight(){return 10;}public void paintIcon(Component c,Graphics g,int x,int y){Graphics2D copy=(Graphics2D)g.create();copy.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);copy.setColor(color);copy.fillOval(x+1,y+1,8,8);copy.dispose();}};}
}
