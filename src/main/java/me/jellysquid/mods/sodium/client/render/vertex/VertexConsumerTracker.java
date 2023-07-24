package me.jellysquid.mods.sodium.client.render.vertex;

import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.render.VertexConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VertexConsumerTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-VertexConsumerTracker");
    private static final ReferenceSet<Class<? extends VertexConsumer>> BAD_CONSUMERS = ReferenceSets.synchronize(new ReferenceOpenHashSet<>());

    public static void logBadConsumer(VertexConsumer consumer) {
        if (BAD_CONSUMERS.add(consumer.getClass())) {
            LOGGER.warn("Class {} does not support optimized vertex writing code paths, which may cause reduced rendering performance",
                    consumer.getClass().getName());
        }
    }
}
