/*
 * Created on Jul 19, 2005
 *
 * Copyright (c) 2005, The JUNG Authors
 *
 * All rights reserved.
 *
 * This software is open-source under the BSD license; see either
 * "license.txt" or
 * https://github.com/jrtom/jung/blob/master/LICENSE for a description.
 */
package mc.client.ui;

import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import com.google.common.base.Function;

import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import lombok.Setter;
import mc.client.graph.GraphNode;

/**
 * Provides a random vertex location within the bounds of the Dimension property.
 * This provides a random location for unmapped vertices
 * the first time they are accessed.
 *
 * @author Tom Nelson
 *
 * @param <V> the vertex type
 */
public class SeededRandomizedLayout<V> implements Function<V,Point2D> {
    private Dimension d;

    public void setDimensions(Dimension d_) {
        this.d = d_;
    }

    private Random random;

    public SeededRandomizedLayout() {
        this.random = new Random(42);
    }


    /**
     * Creates an instance with the specified size which uses the current time
     * as the random seed.
     * @param d the size of the layout area
     *
     *
     */
    public SeededRandomizedLayout(Dimension d) {
        this.d = d;
        this.random = new Random(42);
    }

    public Point2D apply(V v) {
        if(v instanceof GraphNode) {
            this.random = new Random(v.hashCode());

            return new Point2D.Double(50+random.nextDouble() * d.width, 50+random.nextDouble() * d.height);

        } else {return null;}

    }
}
