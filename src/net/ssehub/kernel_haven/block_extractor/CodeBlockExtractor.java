package net.ssehub.kernel_haven.block_extractor;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import net.ssehub.kernel_haven.SetUpException;
import net.ssehub.kernel_haven.code_model.AbstractCodeModelExtractor;
import net.ssehub.kernel_haven.code_model.CodeBlock;
import net.ssehub.kernel_haven.code_model.SourceFile;
import net.ssehub.kernel_haven.config.Configuration;
import net.ssehub.kernel_haven.config.DefaultSettings;
import net.ssehub.kernel_haven.util.CodeExtractorException;
import net.ssehub.kernel_haven.util.ExtractorException;
import net.ssehub.kernel_haven.util.FormatException;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
import net.ssehub.kernel_haven.util.null_checks.Nullable;

/**
 * A code-model extractor that extracts #ifdef blocks from C source files. Creates {@link SourceFile}s with
 * {@link CodeBlock}s.
 *
 * @author Adam
 */
public class CodeBlockExtractor extends AbstractCodeModelExtractor {

    private File sourceTree;
    
    private boolean doLinuxReplacements;
    
    private boolean fuzzyParsing;
    
    @Override
    protected void init(@NonNull Configuration config) throws SetUpException {
        this.sourceTree = config.getValue(DefaultSettings.SOURCE_TREE);
        
        // if the arch setting is set, then we are (probably) analysing Linux
        this.doLinuxReplacements = config.getValue(DefaultSettings.ARCH) != null;
        
        this.fuzzyParsing = config.getValue(DefaultSettings.FUZZY_PARSING);
    }

    @Override
    protected @Nullable SourceFile runOnFile(@NonNull File target) throws ExtractorException {
        File absoulteTarget = new File(sourceTree, target.getPath());
        
        SourceFile result = new SourceFile(target);
        
        try (Parser parser = new Parser(new FileReader(absoulteTarget), target, doLinuxReplacements, fuzzyParsing)) {
            
            for (CodeBlock block : parser.readBlocks()) {
                result.addElement(block);
            }
            
        } catch (IOException e) {
            throw (CodeExtractorException)
                new CodeExtractorException(target, "Can't read " + absoulteTarget).initCause(e);
        } catch (FormatException e) {
            throw new CodeExtractorException(target, e);
        }
        
        return result;
    }

    @Override
    protected @NonNull String getName() {
        return "CodeBlockExtractor";
    }

}
