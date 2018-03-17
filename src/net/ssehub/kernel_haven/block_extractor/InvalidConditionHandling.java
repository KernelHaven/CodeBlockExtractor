package net.ssehub.kernel_haven.block_extractor;

import net.ssehub.kernel_haven.util.logic.True;

/**
 * Different options how to handle invalid / unparseable expressions in the {@link BlockParser}.
 *
 * @author Adam
 */
public enum InvalidConditionHandling {

    /**
     * Throw an exception. This causes the whole file to not be parseable.
     */
    EXCEPTION,
    
    /**
     * Replace the invalid condition with {@link True}.
     */
    TRUE,
    
    /**
     * Replace the invalid condition with a variable called "PARSING_ERROR".
     */
    ERROR_VARIABLE,
    
}
