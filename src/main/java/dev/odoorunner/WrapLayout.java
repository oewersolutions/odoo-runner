package dev.odoorunner;

import java.awt.*;

/** Flow layout whose preferred height includes wrapped rows, so actions do not get clipped. */
final class WrapLayout extends FlowLayout {
    WrapLayout(){super(FlowLayout.LEFT,8,6);}
    @Override public Dimension preferredLayoutSize(Container target){synchronized(target.getTreeLock()){
        int width=target.getWidth();if(width<=0&&target.getParent()!=null)width=target.getParent().getWidth();if(width<=0)width=900;
        Insets insets=target.getInsets();int available=Math.max(1,width-insets.left-insets.right-getHgap()*2);
        int rowWidth=0,rowHeight=0,totalHeight=0,largest=0;
        for(Component child:target.getComponents()){if(!child.isVisible())continue;Dimension size=child.getPreferredSize();if(rowWidth>0&&rowWidth+getHgap()+size.width>available){largest=Math.max(largest,rowWidth);totalHeight+=rowHeight+getVgap();rowWidth=0;rowHeight=0;}if(rowWidth>0)rowWidth+=getHgap();rowWidth+=size.width;rowHeight=Math.max(rowHeight,size.height);}
        return new Dimension(Math.max(largest,rowWidth)+insets.left+insets.right+getHgap()*2,totalHeight+rowHeight+insets.top+insets.bottom+getVgap()*2);
    }}
}
