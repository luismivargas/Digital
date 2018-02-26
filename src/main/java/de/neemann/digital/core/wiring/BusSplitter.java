package de.neemann.digital.core.wiring;

import de.neemann.digital.core.Node;
import de.neemann.digital.core.NodeException;
import de.neemann.digital.core.ObservableValue;
import de.neemann.digital.core.ObservableValues;
import de.neemann.digital.core.element.*;
import de.neemann.digital.draw.elements.PinException;
import de.neemann.digital.lang.Lang;

import static de.neemann.digital.core.element.PinInfo.input;

/**
 * A bus splitter to split a bidirectional data bus
 */
public class BusSplitter extends Node implements Element {

    /**
     * The bus splitters type description
     */
    public static final ElementTypeDescription DESCRIPTION =
            new ElementTypeDescription(BusSplitter.class, input("OE"))
                    .addAttribute(Keys.ROTATE)
                    .addAttribute(Keys.BITS)
                    .addAttribute(Keys.SPLITTER_SPREADING);

    private final int bits;
    private final ObservableValue commonOut;
    private final ObservableValue[] out;
    private final ObservableValue[] in;
    private ObservableValue oeValue;
    private ObservableValue commonIn;
    private boolean oe;
    private long commonD;
    private ObservableValues outputValues;

    /**
     * Creates a new instance
     *
     * @param attr the components attributes
     */
    public BusSplitter(ElementAttributes attr) {
        ObservableValues.Builder builder = new ObservableValues.Builder();
        bits = attr.getBits();
        commonOut = new ObservableValue("D", bits, true)
                .setBidirectional()
                .setPinDescription(DESCRIPTION);
        builder.add(commonOut);
        out = new ObservableValue[bits];
        for (int i = 0; i < bits; i++) {
            out[i] = new ObservableValue("D" + i, 1, true)
                    .setBidirectional()
                    .setDescription(Lang.get("elem_BusSplitter_pin_D_N", i));
            builder.add(out[i]);
        }
        outputValues = builder.build();
        in = new ObservableValue[bits];
    }

    @Override
    public void setInputs(ObservableValues inputs) throws NodeException {
        oeValue = inputs.get(0).checkBits(1, this).addObserverToValue(this);
        commonIn = inputs.get(1).checkBits(bits, this).addObserverToValue(this);
        for (int i = 0; i < bits; i++)
            in[i] = inputs.get(i + 2).checkBits(1, this).addObserverToValue(this);
    }

    @Override
    public void readInputs() throws NodeException {
        oe = oeValue.getBool();
        if (oe) {
            commonD = commonIn.getValue();
        } else {
            commonD = 0;
            long mask = 1;
            for (int i = 0; i < bits; i++) {
                if (in[i].getBool())
                    commonD |= mask;
                mask <<= 1;
            }
        }
    }

    @Override
    public void writeOutputs() throws NodeException {
        if (oe) {
            commonOut.set(0, true);
            long mask = 1;
            for (int i = 0; i < bits; i++) {
                out[i].setBool((commonD & mask) != 0);
                mask <<= 1;
            }
        } else {
            for (int i = 0; i < bits; i++)
                out[i].set(0, true);
            commonOut.set(commonD, false);
        }
    }

    @Override
    public ObservableValues getOutputs() throws PinException {
        return outputValues;
    }
}
