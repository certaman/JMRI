package jmri;

/**
 * Represent an analog I/O on the layout.
 * 
 * @author Daniel Bergqvist Copyright (C) 2018
 */
public interface AnalogIO extends NamedBean {

    /**
     * Is the value an absolute value or a relative value?
     * In both cases, AnalogIO.getMin() and AnalogIO.getMax() tells the
     * limits of the value.
     */
    public enum AbsoluteOrRelative {
        
        ABSOLUTE(Bundle.getMessage("AnalogIO_Absolute")),
        RELATIVE(Bundle.getMessage("AnalogIO_Relative"));
        
        private final String _str;
        
        private AbsoluteOrRelative(String str) {
            _str = str;
        }
        
        @Override
        public String toString() {
            return _str;
        }
    }

    /**
     * Show whether the analog value is stable.
     * 
     * @return true if the analog value is stable
     */
    default public boolean isConsistentValue() {
        return true;
    }
    
    /**
     * Change the commanded value, which results in the relevant command(s)
     * being sent to the hardware. The exception is thrown if there are problems
     * communicating with the layout hardware.
     * <P>
     * The value must be a valid number, not a NaN or infinity number.
     *
     * @param value the desired analog value
     * @throws jmri.JmriException general error when setting the value fails
     * @throws IllegalArgumentException if the value is Float.NaN,
     *                                  Float.NEGATIVE_INFINITY or
     *                                  Float.POSITIVE_INFINITY
     */
    public void setCommandedAnalogValue(float value) throws JmriException;

    /**
     * Query the commanded value. This is a bound parameter, so you can also
     * register a listener to be informed of changes.
     * <P>
     * The result must be a valid number, not a NaN or infinity number.
     *
     * @return the analog value
     */
    public float getCommandedAnalogValue();
    
    /**
     * Query the known analog value. This is a bound parameter, so you can also
     * register a listener to be informed of changes. A result is always
     * returned; if no other feedback method is available, the commanded value
     * will be used.
     * <P>
     * The result must be a valid number, not a NaN or infinity number.
     *
     * @return the known analog value
     */
    default public float getKnownAnalogValue() {
        return getCommandedAnalogValue();
    }
    
    /**
     * Get the minimum value of this AnalogIO.
     */
    public float getMin();
    
    /**
     * Get the maximum value of this AnalogIO.
     */
    public float getMax();
    
    /**
     * Get the resloution of this AnalogIO.
     */
    public float getResolution();

    /**
     * Is this AnalogIO absolute or relative?
     */
    public AbsoluteOrRelative getAbsoluteOrRelative();

    /**
     * Request an update from the layout soft/hardware. May not even happen, and
     * if it does it will happen later; listen for the result.
     */
    default public void requestUpdateFromLayout() {
    }

}
