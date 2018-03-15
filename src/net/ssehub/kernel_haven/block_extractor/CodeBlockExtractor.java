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
    
    @Override
    protected void init(@NonNull Configuration config) throws SetUpException {
        this.sourceTree = config.getValue(DefaultSettings.SOURCE_TREE);
    }

    @Override
    protected @Nullable SourceFile runOnFile(@NonNull File target) throws ExtractorException {
        
        File absoulteTarget = new File(sourceTree, target.getPath());
        if (!absoulteTarget.isFile()) {
            throw new ExtractorException("Could not parse file, because it does not exist: "
                    + absoulteTarget.getAbsolutePath());
        }
        
        SourceFile result = new SourceFile(target);
        
        try (Parser parser = new Parser(new FileReader(absoulteTarget), target)) {
            
            for (CodeBlock block : parser.readBlocks()) {
                result.addElement(block);
            }
            
        } catch (IOException e) {
            throw new ExtractorException("Can't read " + absoulteTarget, e);
        } catch (FormatException e) {
            throw new ExtractorException("Can't parse " + target, e);
        }
        
        return result;
    }

    @Override
    protected @NonNull String getName() {
        return "CodeBlockExtractor";
    }

}
