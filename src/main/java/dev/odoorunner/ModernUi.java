package dev.odoorunner;

import javax.swing.*;
import java.awt.*;

/** Layout polish only: all surfaces, selections and inputs retain native IDE colors. */
final class ModernUi {
    static void sizeButton(JButton button){if(button==null)return;boolean icon=button.getText()==null||button.getText().isBlank();Dimension size=new Dimension(icon?32:Boolean.TRUE.equals(button.getClientProperty("odoo.compact"))?72:144,32);button.setPreferredSize(size);button.setMinimumSize(size);button.setMaximumSize(size);}
    static void sizeInput(JComponent input){input.setPreferredSize(new Dimension(320,32));input.setMinimumSize(new Dimension(80,32));input.setMaximumSize(new Dimension(Integer.MAX_VALUE,32));}
    static void apply(Component component){
        if(component instanceof JTextField field){field.setMargin(new Insets(5,8,5,8));sizeInput(field);if(SwingUtilities.getAncestorOfClass(JComboBox.class,field)==null)RoundedNativeBorder.install(field);else field.setBorder(BorderFactory.createEmptyBorder(4,8,4,8));}
        if(component instanceof JComboBox<?> combo){sizeInput(combo);RoundedNativeBorder.install(combo);}
        if(component instanceof JButton button){button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));if(button.getClass()==JButton.class||Boolean.TRUE.equals(button.getClientProperty("odoo.button")))sizeButton(button);if(button.getText()!=null&&!button.getText().isBlank())button.setMargin(new Insets(5,10,5,10));if(Boolean.TRUE.equals(button.getClientProperty("odoo.primary")))Design.primary(button);if(button.getClass()==JButton.class||Boolean.TRUE.equals(button.getClientProperty("odoo.button")))RoundedNativeBorder.install(button);}
        if(component instanceof JTable table){table.setRowHeight(32);table.setShowGrid(false);}
        if(component instanceof Container container)for(Component child:container.getComponents())apply(child);
    }
}
