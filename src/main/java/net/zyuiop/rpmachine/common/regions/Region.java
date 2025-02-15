package net.zyuiop.rpmachine.common.regions;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Spliterator;
import java.util.Spliterators;

/**
 * @author Louis Vialar
 */
public interface Region extends Iterable<Block> {
    boolean isInside(Location location);

    int computeArea();

    int computeVolume();

    boolean hasBlockInChunk(Chunk chunk);


    /**
     * Expand the region in height
     * @param y the height to add, if negative height will be added at the bottom
     */
    void expandY(int y);

    @Override
    default Spliterator<Block> spliterator() {
        return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.CONCURRENT | Spliterator.IMMUTABLE);
    }
}
