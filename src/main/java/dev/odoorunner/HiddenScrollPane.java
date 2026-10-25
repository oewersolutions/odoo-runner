package dev.odoorunner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Native precision-trackpad movement, eased mouse-wheel steps, and scroll chaining. */
final class HiddenScrollPane extends JScrollPane {
    private double targetY,targetX;
    private final Timer animation;
    HiddenScrollPane(Component content){
        super(content);setVerticalScrollBar(hiddenBar(JScrollBar.VERTICAL));setHorizontalScrollBar(hiddenBar(JScrollBar.HORIZONTAL));setWheelScrollingEnabled(false);enableEvents(AWTEvent.MOUSE_WHEEL_EVENT_MASK);setBorder(BorderFactory.createEmptyBorder());getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        animation=new Timer(16,e->step());animation.setCoalesce(true);addHierarchyListener(e->{if((e.getChangeFlags()&HierarchyEvent.SHOWING_CHANGED)!=0&&!isShowing())animation.stop();});
    }
    private int maximum(boolean horizontal){Component view=getViewport().getView();Dimension extent=getViewport().getExtentSize();return view==null?0:Math.max(0,(horizontal?view.getWidth():view.getHeight())-(horizontal?extent.width:extent.height));}
    private void step(){Point position=getViewport().getViewPosition();targetY=Math.max(0,Math.min(maximum(false),targetY));targetX=Math.max(0,Math.min(maximum(true),targetX));double dx=targetX-position.x,dy=targetY-position.y;position.x+=Math.abs(dx)<=2?(int)Math.round(dx):(int)Math.copySign(Math.max(1,Math.abs(dx)*0.35),dx);position.y+=Math.abs(dy)<=2?(int)Math.round(dy):(int)Math.copySign(Math.max(1,Math.abs(dy)*0.35),dy);getViewport().setViewPosition(position);if(Math.abs(targetX-position.x)<1&&Math.abs(targetY-position.y)<1)animation.stop();}
    protected void processMouseWheelEvent(MouseWheelEvent event){
        if(event.isConsumed()||getViewport().getView()==null)return;boolean horizontal=event.isShiftDown();Point position=getViewport().getViewPosition();int current=horizontal?position.x:position.y,max=maximum(horizontal);
        double rotation=event.getPreciseWheelRotation();double delta=rotation*(event.getScrollType()==MouseWheelEvent.WHEEL_BLOCK_SCROLL?Math.max(24,getViewport().getExtentSize().height-48):Math.max(1,event.getScrollAmount())*12);
        if((delta>0&&current<max)||(delta<0&&current>0)){
            boolean precise=Math.abs(rotation-Math.rint(rotation))>0.001;
            if(precise){animation.stop();int next=(int)Math.round(Math.max(0,Math.min(max,current+delta)));if(horizontal)position.x=next;else position.y=next;getViewport().setViewPosition(position);}
            else{if(!animation.isRunning()){targetX=position.x;targetY=position.y;}if(horizontal)targetX=Math.max(0,Math.min(max,targetX+delta));else targetY=Math.max(0,Math.min(max,targetY+delta));animation.start();}
            event.consume();return;
        }
        Container parent=getParent();while(parent!=null&&!(parent instanceof JScrollPane))parent=parent.getParent();if(parent!=null){Point point=SwingUtilities.convertPoint(this,event.getPoint(),parent);parent.dispatchEvent(new MouseWheelEvent(parent,event.getID(),event.getWhen(),event.getModifiersEx(),point.x,point.y,event.getXOnScreen(),event.getYOnScreen(),event.getClickCount(),event.isPopupTrigger(),event.getScrollType(),event.getScrollAmount(),event.getWheelRotation(),rotation));}event.consume();
    }
    private JScrollBar hiddenBar(int orientation){JScrollBar bar=new JScrollBar(orientation){public Dimension getPreferredSize(){return new Dimension(0,0);}public Dimension getMinimumSize(){return new Dimension(0,0);}public Dimension getMaximumSize(){return new Dimension(0,0);}};bar.setUnitIncrement(16);bar.setFocusable(false);return bar;}
}
