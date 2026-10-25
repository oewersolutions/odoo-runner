package dev.odoorunner;

import java.awt.*;

/** Resizing a popup must not measure/layout every hidden settings page. */
final class VisibleCardLayout extends CardLayout {
    public Dimension preferredLayoutSize(Container parent){return size(parent,false);}
    public Dimension minimumLayoutSize(Container parent){return size(parent,true);}
    private Dimension size(Container parent,boolean minimum){Insets insets=parent.getInsets();for(Component child:parent.getComponents())if(child.isVisible()){Dimension size=minimum?child.getMinimumSize():child.getPreferredSize();return new Dimension(size.width+insets.left+insets.right,size.height+insets.top+insets.bottom);}return new Dimension(360,120);}
    public void layoutContainer(Container parent){Insets insets=parent.getInsets();for(Component child:parent.getComponents())if(child.isVisible())child.setBounds(insets.left,insets.top,Math.max(0,parent.getWidth()-insets.left-insets.right),Math.max(0,parent.getHeight()-insets.top-insets.bottom));}
}
