package dev.odoorunner;

import javax.swing.*;
import java.awt.*;

/** A single width-tracking vertical form; sections retain their natural height. */
final class ScrollForm extends JPanel implements Scrollable {
    ScrollForm(){setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));setBorder(BorderFactory.createEmptyBorder(10,10,10,10));}
    void section(JComponent component){component.setAlignmentX(LEFT_ALIGNMENT);add(component);add(Box.createVerticalStrut(10));}
    public Dimension getPreferredScrollableViewportSize(){return new Dimension(850,640);}
    public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction){return 24;}
    public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction){return Math.max(24,visible.height-48);}
    public boolean getScrollableTracksViewportWidth(){return true;}
    public boolean getScrollableTracksViewportHeight(){return false;}
    static JPanel group(String title){
        return new Design.Card(new GridBagLayout(),12){
            final Icon icon=title.contains("source")?UiIcons.CODE:title.contains("Database")?UiIcons.DATABASE:title.equals("Tests")?UiIcons.TEST:title.contains("Development")?UiIcons.GEAR:title.contains("notes")?UiIcons.LOG:title.contains("director")?UiIcons.FOLDER:UiIcons.SERVER;
            {setBorder(BorderFactory.createEmptyBorder(34,58,10,12));setAlignmentX(LEFT_ALIGNMENT);}
            @Override protected void paintComponent(Graphics graphics){super.paintComponent(graphics);Graphics2D g=(Graphics2D)graphics.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setColor(Design.BLUE);g.drawOval(8,8,32,32);icon.paintIcon(this,g,16,16);g.setColor(Design.TEXT);g.setFont(getFont().deriveFont(Font.BOLD,13f));g.drawString(title,66,23);g.dispose();}
            @Override public Dimension getMaximumSize(){return new Dimension(Integer.MAX_VALUE,getPreferredSize().height);}
        };
    }
    static void field(JPanel group,String label,JComponent control){
        int row=group.getComponentCount()/2;
        GridBagConstraints c=new GridBagConstraints();c.gridy=row;c.gridx=0;c.anchor=GridBagConstraints.NORTHWEST;c.insets=new Insets(4,8,4,8);c.fill=GridBagConstraints.HORIZONTAL;
        JLabel heading=new JLabel(label);heading.setLabelFor(control);heading.setPreferredSize(new Dimension(200,Math.max(30,heading.getPreferredSize().height)));heading.setToolTipText(label);group.add(heading,c);
        c.gridx=1;c.weightx=1;group.add(control,c);
    }
}
