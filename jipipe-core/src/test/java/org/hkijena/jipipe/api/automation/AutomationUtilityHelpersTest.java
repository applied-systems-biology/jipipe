package org.hkijena.jipipe.api.automation;

import org.hkijena.jipipe.api.automation.operations.AutomationOperationUtils;
import org.hkijena.jipipe.plugins.expressions.JIPipeExpressionParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeDynamicEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.quantities.Quantity;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AutomationUtilityHelpersTest {

    @Test
    void unsupportedTypeReturnsNull() {
        assertNull(AutomationOperationUtils.parseCompositeValue("x", String[].class));
    }

    @Test
    void colorAcceptsSixDigitHex() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("#FF8000", Color.class);
        assertEquals(new Color(255, 128, 0), parsed);
    }

    @Test
    void colorAcceptsEightDigitHexWithAlpha() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("#FF800080", Color.class);
        assertEquals(new Color(255, 128, 0, 128), parsed);
    }

    @Test
    void eightDigitHexIsAlphaLastNotAlphaFirst() {
        Color parsed = (Color) AutomationOperationUtils.parseCompositeValue("#FF800080", Color.class);
        assertEquals(128, parsed.getAlpha());
        assertEquals(255, parsed.getRed());
        assertNotEquals(new Color(128, 0, 128, 255), parsed);
    }

    @Test
    void malformedColorNamesTheAcceptedEncoding() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AutomationOperationUtils.parseCompositeValue("orange-ish", Color.class));
        assertTrue(e.getMessage().contains("#RRGGBB"));
    }

    @Test
    void expressionAcceptsAValidExpressionString() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("x > 1", JIPipeExpressionParameter.class);
        assertInstanceOf(JIPipeExpressionParameter.class, parsed);
        assertEquals("x > 1", ((JIPipeExpressionParameter) parsed).getExpression());
    }

    @Test
    void malformedExpressionNamesTheAcceptedEncoding() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AutomationOperationUtils.parseCompositeValue("(x > 1", JIPipeExpressionParameter.class));
        assertTrue(e.getMessage().contains("expression"));
    }

    @Test
    void quantityAcceptsValueAndUnit() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("5 px", Quantity.class);
        assertEquals(new Quantity(5, "px"), parsed);
    }

    @Test
    void quantityAcceptsABareValue() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("2.5", Quantity.class);
        assertEquals(new Quantity(2.5, ""), parsed);
    }

    @Test
    void malformedQuantityNamesTheAcceptedEncoding() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AutomationOperationUtils.parseCompositeValue("a lot", Quantity.class));
        assertTrue(e.getMessage().contains("<value> <unit>"));
    }

    @Test
    void dynamicEnumAcceptsARegisteredItem() {
        Object parsed = AutomationOperationUtils.parseCompositeValue("beta", TestItemsEnumParameter.class);
        assertInstanceOf(TestItemsEnumParameter.class, parsed);
        assertEquals("beta", ((TestItemsEnumParameter) parsed).getValue());
    }

    @Test
    void malformedDynamicEnumNamesTheRegisteredItems() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> AutomationOperationUtils.parseCompositeValue("gamma", TestItemsEnumParameter.class));
        assertTrue(e.getMessage().contains("alpha, beta"));
    }

    /**
     * Test double of a dynamic enum parameter whose items are registered in the constructor
     * (the shape of e.g. JIPipeModernThemeStyleParameter).
     */
    private static final class TestItemsEnumParameter extends JIPipeDynamicEnumParameter<String> {
        public TestItemsEnumParameter() {
            setAllowedValues(List.of("alpha", "beta"));
        }
    }
}
