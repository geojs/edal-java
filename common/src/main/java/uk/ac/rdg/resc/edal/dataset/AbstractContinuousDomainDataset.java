/*******************************************************************************
 * Copyright (c) 2014 The University of Reading
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the University of Reading, nor the names of the
 *    authors or contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/

package uk.ac.rdg.resc.edal.dataset;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;

import uk.ac.rdg.resc.edal.domain.Extent;
import uk.ac.rdg.resc.edal.exceptions.DataReadingException;
import uk.ac.rdg.resc.edal.feature.DiscreteFeature;
import uk.ac.rdg.resc.edal.feature.PointSeriesFeature;
import uk.ac.rdg.resc.edal.feature.ProfileFeature;
import uk.ac.rdg.resc.edal.geometry.BoundingBox;
import uk.ac.rdg.resc.edal.metadata.VariableMetadata;
import uk.ac.rdg.resc.edal.util.GISUtils;
import uk.ac.rdg.resc.edal.util.PlottingDomainParams;

/**
 * Partial implementation of a {@link ContinuousDomainDataset} which performs
 * spatial indexing of features.
 * 
 * @author Guy Griffiths
 * 
 * @param <F>
 *            The type of {@link DiscreteFeature} contained in this
 *            {@link AbstractContinuousDomainDataset}
 */
public abstract class AbstractContinuousDomainDataset extends AbstractDataset {

    private Class<? extends DiscreteFeature<?, ?>> featureType;
    private FeatureIndexer featureIndexer;

    public AbstractContinuousDomainDataset(String id, Collection<? extends VariableMetadata> vars,
            Class<? extends DiscreteFeature<?, ?>> featureType, FeatureIndexer featureIndexer) {
        super(id, vars);
        this.featureType = featureType;
        this.featureIndexer = featureIndexer;
    }

    @Override
    public DiscreteFeature<?, ?> readFeature(String featureId) throws DataReadingException {
        return getFeatureReader().readFeature(featureId, null);
    }

    @Override
    public Set<String> getFeatureIds() {
        return featureIndexer.getAllFeatureIds();
    }

    @Override
    public Collection<? extends DiscreteFeature<?, ?>> extractMapFeatures(Set<String> varIds,
            PlottingDomainParams params) throws DataReadingException {
        BoundingBox hExtent = params.getBbox();
        Extent<Double> zExtent = params.getZExtent();
        Extent<DateTime> tExtent = params.getTExtent();

        if (hExtent == null) {
            hExtent = getDatasetBoundingBox();
        }
        if (zExtent == null) {
            zExtent = getDatasetVerticalExtent();
        }
        if (tExtent == null) {
            tExtent = getDatasetTimeExtent();
        }
        /*
         * Extend the bounding box by 5% so that we include any features which
         * are just off the map. This may not work if we are tiling very small
         * images or using very big icons (i.e. cases where 5% of the total
         * width < half the icon size). Then we will see artifacts at tile
         * boundaries.
         */
        BoundingBox largeBoundingBox = GISUtils.getLargeBoundingBox(hExtent, 5);
        List<DiscreteFeature<?, ?>> features = new ArrayList<DiscreteFeature<?, ?>>();
        Collection<String> featureIds = featureIndexer.findFeatureIds(largeBoundingBox, zExtent,
                tExtent, varIds);

        features.addAll(getFeatureReader().readFeatures(featureIds, varIds));
        return features;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends ProfileFeature> extractProfileFeatures(Set<String> varIds,
            PlottingDomainParams params) throws DataReadingException {
        if (!ProfileFeature.class.isAssignableFrom(getMapFeatureType())) {
            throw new UnsupportedOperationException(
                    "This dataset does not support profile features");
        }
        List<ProfileFeature> features = new ArrayList<ProfileFeature>();
        Collection<String> featureIds = featureIndexer.findFeatureIds(params.getBbox(),
                params.getZExtent(), params.getTExtent(), varIds);
        features.addAll((Collection<? extends ProfileFeature>) getFeatureReader().readFeatures(
                featureIds, varIds));
        return features;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<? extends PointSeriesFeature> extractTimeseriesFeatures(Set<String> varIds,
            PlottingDomainParams params) throws DataReadingException {
        if (!PointSeriesFeature.class.isAssignableFrom(getMapFeatureType())) {
            throw new UnsupportedOperationException(
                    "This dataset does not support time series features");
        }
        List<PointSeriesFeature> features = new ArrayList<PointSeriesFeature>();
        Collection<String> featureIds = featureIndexer.findFeatureIds(params.getBbox(),
                params.getZExtent(), params.getTExtent(), varIds);
        features.addAll((Collection<? extends PointSeriesFeature>) getFeatureReader().readFeatures(
                featureIds, varIds));
        return features;
    }

    /**
     * @return The {@link BoundingBox} of the entire dataset
     */
    protected abstract BoundingBox getDatasetBoundingBox();

    /**
     * @return The {@link Extent} of the vertical domain for the entire dataset
     */
    protected abstract Extent<Double> getDatasetVerticalExtent();

    /**
     * @return The {@link Extent} of the time domain for the entire dataset
     */
    protected abstract Extent<DateTime> getDatasetTimeExtent();

    @Override
    public Class<? extends DiscreteFeature<?, ?>> getMapFeatureType() {
        return featureType;
    }

    public abstract DiscreteFeatureReader<? extends DiscreteFeature<?, ?>> getFeatureReader();
}