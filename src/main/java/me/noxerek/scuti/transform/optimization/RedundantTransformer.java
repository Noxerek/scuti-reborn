package me.noxerek.scuti.transform.optimization;

import me.noxerek.scuti.configuration.Configuration;
import me.noxerek.scuti.transform.Transformer;
import org.objectweb.asm.tree.ClassNode;

import java.util.Map;

public class RedundantTransformer extends Transformer {

    public RedundantTransformer(final Configuration configuration, final Map<String, ClassNode> classes,
                                final Map<String, ClassNode> dependencies) {
        super(configuration, classes, dependencies);
    }

    @Override
    public void transform() {

    }

}
