/*
 * Copyright (C) 2012-2018 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation.
 *
 * If the program is linked with libraries which are licensed under one of
 * the following licenses, the combination of the program with the linked
 * library is not considered a "derivative work" of the program:
 *
 *     - Apache License, version 2.0
 *     - Apache Software License, version 1.0
 *     - GNU Lesser General Public License, version 3
 *     - Mozilla Public License, versions 1.0, 1.1 and 2.0
 *     - Common Development and Distribution License (CDDL), version 1.0
 *
 * Therefore the distribution of the program linked with libraries licensed
 * under the aforementioned licenses, is permitted by the copyright holders
 * if the distribution is compliant with both the GNU General Public
 * License version 2 and the aforementioned licenses.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 */
package org.n52.sos.ds.hibernate.cache.base;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.internal.util.collections.CollectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.n52.iceland.ds.ConnectionProvider;
import org.n52.iceland.i18n.I18NDAORepository;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.sos.ds.FeatureQueryHandler;
import org.n52.sos.ds.hibernate.cache.AbstractQueueingDatasourceCacheUpdate;
import org.n52.sos.ds.hibernate.dao.DaoFactory;
import org.n52.sos.ds.hibernate.dao.observation.AbstractObservationDAO;
import org.n52.sos.ds.hibernate.entities.FeatureOfInterestType;
import org.n52.sos.ds.hibernate.entities.ObservationType;
import org.n52.sos.ds.hibernate.entities.Offering;
import org.n52.sos.ds.hibernate.entities.RelatedFeature;
import org.n52.sos.ds.hibernate.entities.TOffering;
import org.n52.sos.ds.hibernate.util.ObservationConstellationInfo;
import org.n52.sos.ds.hibernate.util.OfferingTimeExtrema;

import com.google.common.collect.Lists;

/**
 *
 * @author <a href="mailto:c.autermann@52north.org">Christian Autermann</a>
 *
 * @since 4.0.0
 */
public class OfferingCacheUpdate extends AbstractQueueingDatasourceCacheUpdate<OfferingCacheUpdateTask> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferingCacheUpdate.class);
    private static final String THREAD_GROUP_NAME = "offering-cache-update";
    private Collection<String> offeringsIdToUpdate = Lists.newArrayList();
    private Collection<Offering> offeringsToUpdate;
    private Map<String,Collection<ObservationConstellationInfo>> offObsConstInfoMap;
    private final Locale defaultLanguage;
    private final I18NDAORepository i18NDAORepository;
    private final FeatureQueryHandler featureQueryHandler;
    private final DaoFactory daoFactory;

    public OfferingCacheUpdate(int threads, Locale defaultLanguage, I18NDAORepository i18NDAORepository, FeatureQueryHandler featureQueryHandler, ConnectionProvider connectionProvider, DaoFactory daoFactory) {
        this(threads, defaultLanguage, i18NDAORepository, featureQueryHandler, connectionProvider, null, daoFactory);
    }

    public OfferingCacheUpdate(int threads, Locale defaultLanguage, I18NDAORepository i18NDAORepository, FeatureQueryHandler featureQueryHandler, ConnectionProvider connectionProvider, Collection<String> offeringIdsToUpdate, DaoFactory daoFactory) {
        super(threads, THREAD_GROUP_NAME, connectionProvider);
        this.offeringsIdToUpdate = offeringIdsToUpdate;
        this.defaultLanguage = defaultLanguage;
        this.i18NDAORepository = i18NDAORepository;
        this.featureQueryHandler = featureQueryHandler;
        this.daoFactory = daoFactory;
    }

    private Collection<Offering> getOfferingsToUpdate() {
        if (offeringsToUpdate == null) {
            offeringsToUpdate = daoFactory.getOfferingDAO().getOfferingObjectsForCacheUpdate(offeringsIdToUpdate, getSession());
        }
        return offeringsToUpdate;
    }

    private Map<String,Collection<ObservationConstellationInfo>> getOfferingObservationConstellationInfo() {
        if (offObsConstInfoMap == null) {
            offObsConstInfoMap = ObservationConstellationInfo.mapByOffering(
                daoFactory.getObservationConstellationDAO().getObservationConstellationInfo(getSession()));
        }
        return offObsConstInfoMap;
    }

    @Override
    public void execute() {
        LOGGER.debug("Executing OfferingCacheUpdate (Single Threaded Tasks)");
        startStopwatch();
        //perform single threaded updates here
        Map<String, Collection<String>> offeringMap = offeringDAO.getOfferingIdentifiers(getSession());
        for (Offering offering : getOfferingsToUpdate()) {
            String offeringId = offering.getIdentifier();
            if (shouldOfferingBeProcessed(offeringId)) {
                getCache().addOffering(offeringId);
                getCache().setAllowedObservationTypeForOffering(offeringId,
                        getObservationTypesFromObservationType(offering.getObservationTypes()));

                if (offering instanceof TOffering) {
                    TOffering tOffering = (TOffering) offering;
                    // Related features
                    Set<String> relatedFeatures = getRelatedFeatureIdentifiersFrom(tOffering);
                    if (!relatedFeatures.isEmpty()) {
                        getCache().setRelatedFeaturesForOffering(offeringId, relatedFeatures);
                    }
                    getCache().setAllowedObservationTypeForOffering(offeringId,
                            getObservationTypesFromObservationType(tOffering.getObservationTypes()));
                    // featureOfInterestTypes
                    getCache().setAllowedFeatureOfInterestTypeForOffering(offeringId,
                            getFeatureOfInterestTypesFromFeatureOfInterestType(tOffering.getFeatureOfInterestTypes()));
                }
                Collection<String> parentOfferings = offeringMap.get(offeringId);
                if (!CollectionHelper.isEmpty(parentOfferings)) {
                    getCache().addParentOfferings(offeringId, parentOfferings);
                }
            }
        }

        //time ranges
        //TODO querying offering time extrema in a single query is definitely faster for a properly
        //     indexed Postgres db, but may not be true for all platforms. move back to multithreaded execution
        //     in OfferingCacheUpdateTask if needed
        Map<String, OfferingTimeExtrema> offeringTimeExtrema = null;
        try {
            offeringTimeExtrema = daoFactory.getOfferingDAO().getOfferingTimeExtrema(offeringsIdToUpdate, getSession());
        } catch (OwsExceptionReport ce) {
            LOGGER.error("Error while querying offering time ranges!", ce);
            getErrors().copy(ce);
        }
        if (!CollectionHelper.isEmpty(offeringTimeExtrema)) {
            for (Entry<String, OfferingTimeExtrema> entry : offeringTimeExtrema.entrySet()) {
                String offeringId = entry.getKey();
                OfferingTimeExtrema ote = entry.getValue();
                getCache().setMinPhenomenonTimeForOffering(offeringId, ote.getMinPhenomenonTime());
                getCache().setMaxPhenomenonTimeForOffering(offeringId, ote.getMaxPhenomenonTime());
                getCache().setMinResultTimeForOffering(offeringId, ote.getMinResultTime());
                getCache().setMaxResultTimeForOffering(offeringId, ote.getMaxResultTime());
            }
        }
        
        LOGGER.debug("Finished executing OfferingCacheUpdate (Single Threaded Tasks) ({})", getStopwatchResult());

        //execute multi-threaded updates
        LOGGER.debug("Executing OfferingCacheUpdate (Multi-Threaded Tasks)");
        startStopwatch();
        super.execute();
        LOGGER.debug("Finished executing OfferingCacheUpdate (Multi-Threaded Tasks) ({})", getStopwatchResult());
    }
    

    @Override
    protected OfferingCacheUpdateTask[] getUpdatesToExecute() throws OwsExceptionReport {
        Collection<OfferingCacheUpdateTask> offeringUpdateTasks = Lists.newArrayList();
        boolean hasSamplingGeometry = checkForSamplingGeometry();
        for (Offering offering : getOfferingsToUpdate()){
            
            if (shouldOfferingBeProcessed(offering.getIdentifier())) {
                Collection<ObservationConstellationInfo> observationConstellations
                        = getOfferingObservationConstellationInfo().get(offering.getIdentifier());
                offeringUpdateTasks.add(new OfferingCacheUpdateTask(
                        offering,
                        observationConstellations,
                        hasSamplingGeometry,
                        this.defaultLanguage,
                        this.i18NDAORepository,
                        this.featureQueryHandler,
                        this.daoFactory));
            }
        }
        return offeringUpdateTasks.toArray(new OfferingCacheUpdateTask[offeringUpdateTasks.size()]);
    }

    /**
     * Check if the observation table contains samplingGeometries with values.
     *
     * @return <code>true</code>, if the observation table contains samplingGeometries with values
     */
    private boolean checkForSamplingGeometry() {
        try {
            AbstractObservationDAO observationDAO = daoFactory.getObservationDAO();
            return observationDAO.containsSamplingGeometries(getSession());
        } catch (OwsExceptionReport e) {
            LOGGER.error("Error while getting observation DAO class from factory!", e);
            getErrors().copy(e);
        }
        return false;
    }

    protected boolean shouldOfferingBeProcessed(String offeringIdentifier) {
     // TODO support for Offering Hierarchy!!!
        return true;
//        try {
//            // TODO support for Offering Hierarchy!!!
//            if (HibernateHelper.isEntitySupported(ObservationConstellation.class)) {
//                return getOfferingObservationConstellationInfo().containsKey(offeringIdentifier);
//            } else {
//                AbstractObservationDAO observationDAO = DaoFactory.getInstance().getObservationDAO();
//                Criteria criteria = observationDAO.getDefaultObservationInfoCriteria(getSession());
//                criteria.createCriteria(AbstractObservation.OFFERINGS).add(
//                        Restrictions.eq(Offering.IDENTIFIER, offeringIdentifier));
//                criteria.setProjection(Projections.rowCount());
//                LOGGER.debug("QUERY shouldOfferingBeProcessed(offering): {}", HibernateHelper.getSqlString(criteria));
//                return (Long) criteria.uniqueResult() > 0;
//            }
//        } catch (OwsExceptionReport e) {
//            LOGGER.error("Error while getting observation DAO class from factory!", e);
//            getErrors().add(e);
//        }
//        return false;
    }

    protected Set<String> getObservationTypesFromObservationType(Set<ObservationType> observationTypes) {
        Set<String> obsTypes = new HashSet<>(observationTypes.size());
        for (ObservationType obsType : observationTypes) {
            obsTypes.copy(obsType.getObservationType());
        }
        return obsTypes;
    }

    protected Collection<String> getFeatureOfInterestTypesFromFeatureOfInterestType(
            Set<FeatureOfInterestType> featureOfInterestTypes) {
        Set<String> featTypes = new HashSet<>(featureOfInterestTypes.size());
        for (FeatureOfInterestType featType : featureOfInterestTypes) {
            featTypes.copy(featType.getFeatureOfInterestType());
        }
        return featTypes;
    }

    protected Set<String> getRelatedFeatureIdentifiersFrom(TOffering hOffering) {
        Set<String> relatedFeatureList = new HashSet<>(hOffering.getRelatedFeatures().size());
        for (RelatedFeature hRelatedFeature : hOffering.getRelatedFeatures()) {
            if (hRelatedFeature.getFeatureOfInterest() != null
                    && hRelatedFeature.getFeatureOfInterest().getIdentifier() != null) {
                relatedFeatureList.copy(hRelatedFeature.getFeatureOfInterest().getIdentifier());
            }
        }
        return relatedFeatureList;
    }
}
