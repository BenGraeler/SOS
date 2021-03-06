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
package org.n52.sos.ds.hibernate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.opengis.sensorML.x101.SystemDocument;
import net.opengis.swe.x20.DataRecordDocument;
import net.opengis.swe.x20.TextEncodingDocument;

import org.apache.xmlbeans.XmlObject;
import org.hibernate.Session;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

import org.n52.iceland.convert.ConverterException;
import org.n52.iceland.convert.ConverterRepository;
import org.n52.iceland.i18n.I18NDAORepository;
import org.n52.iceland.ogc.ows.OwsServiceProviderFactory;
import org.n52.janmayen.event.EventBus;
import org.n52.shetland.ogc.filter.FilterConstants;
import org.n52.shetland.ogc.filter.TemporalFilter;
import org.n52.shetland.ogc.gml.AbstractFeature;
import org.n52.shetland.ogc.gml.CodeWithAuthority;
import org.n52.shetland.ogc.gml.ReferenceType;
import org.n52.shetland.ogc.gml.time.IndeterminateValue;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.om.NamedValue;
import org.n52.shetland.ogc.om.ObservationValue;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservableProperty;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.OmObservationConstellation;
import org.n52.shetland.ogc.om.SingleObservationValue;
import org.n52.shetland.ogc.om.StreamingValue;
import org.n52.shetland.ogc.om.features.SfConstants;
import org.n52.shetland.ogc.om.features.samplingFeatures.SamplingFeature;
import org.n52.shetland.ogc.om.values.QuantityValue;
import org.n52.shetland.ogc.om.values.SweDataArrayValue;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.ows.service.OwsServiceRequestContext;
import org.n52.shetland.ogc.sensorML.SensorMLConstants;
import org.n52.shetland.ogc.sensorML.System;
import org.n52.shetland.ogc.sos.Sos2Constants;
import org.n52.shetland.ogc.sos.SosConstants;
import org.n52.shetland.ogc.sos.SosInsertionMetadata;
import org.n52.shetland.ogc.sos.SosOffering;
import org.n52.shetland.ogc.sos.SosProcedureDescription;
import org.n52.shetland.ogc.sos.SosResultEncoding;
import org.n52.shetland.ogc.sos.SosResultStructure;
import org.n52.shetland.ogc.sos.request.DeleteSensorRequest;
import org.n52.shetland.ogc.sos.request.GetObservationRequest;
import org.n52.shetland.ogc.sos.request.InsertObservationRequest;
import org.n52.shetland.ogc.sos.request.InsertResultRequest;
import org.n52.shetland.ogc.sos.request.InsertResultTemplateRequest;
import org.n52.shetland.ogc.sos.request.InsertSensorRequest;
import org.n52.shetland.ogc.sos.response.DeleteSensorResponse;
import org.n52.shetland.ogc.sos.response.GetObservationResponse;
import org.n52.shetland.ogc.sos.response.InsertObservationResponse;
import org.n52.shetland.ogc.sos.response.InsertResultResponse;
import org.n52.shetland.ogc.sos.response.InsertResultTemplateResponse;
import org.n52.shetland.ogc.sos.response.InsertSensorResponse;
import org.n52.shetland.ogc.swe.SweConstants;
import org.n52.shetland.ogc.swe.SweDataArray;
import org.n52.shetland.ogc.swe.SweDataRecord;
import org.n52.shetland.ogc.swe.SweField;
import org.n52.shetland.ogc.swe.encoding.SweTextEncoding;
import org.n52.shetland.ogc.swe.simpleType.SweBoolean;
import org.n52.shetland.ogc.swe.simpleType.SweCount;
import org.n52.shetland.ogc.swe.simpleType.SweQuantity;
import org.n52.shetland.ogc.swe.simpleType.SweTime;
import org.n52.shetland.ogc.swes.SwesExtension;
import org.n52.shetland.ogc.swes.SwesExtensions;
import org.n52.shetland.util.CollectionHelper;
import org.n52.sos.cache.SosContentCache;
import org.n52.sos.ds.hibernate.dao.DaoFactory;
import org.n52.sos.ds.hibernate.dao.ProcedureDAO;
import org.n52.sos.ds.hibernate.entities.Procedure;
import org.n52.sos.ds.hibernate.util.TemporalRestrictions;
import org.n52.sos.ds.hibernate.util.procedure.HibernateProcedureConverter;
import org.n52.sos.ds.hibernate.util.procedure.HibernateProcedureCreationContext;
import org.n52.sos.ds.hibernate.util.procedure.generator.HibernateProcedureDescriptionGeneratorFactoryRepository;
import org.n52.sos.event.events.ObservationInsertion;
import org.n52.sos.event.events.ResultInsertion;
import org.n52.sos.event.events.ResultTemplateInsertion;
import org.n52.sos.event.events.SensorDeletion;
import org.n52.sos.event.events.SensorInsertion;
import org.n52.sos.request.operator.SosInsertObservationOperatorV20;
import org.n52.sos.service.Configurator;
import org.n52.svalbard.decode.exception.DecodingException;
import org.n52.svalbard.encode.EncoderRepository;
import org.n52.svalbard.encode.exception.EncodingException;
import org.n52.svalbard.util.CodingHelper;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

/**
 * Test various Insert*DAOs using a common set of test data with hierarchical
 * procedures. NOTE: These tests fail intermittently. They have been excluded
 * from the normal build and set up to run multiple (100) times. They can be run
 * directly from Eclipse or via Maven on the command line with the dao-test
 * profile (mvn -P dao-test clean install)
 *
 * @author <a href="mailto:shane@axiomalaska.com">Shane StClair</a>
 *
 * @since 4.0.0
 *
 */
@RunWith(Parameterized.class)
public class InsertDAOTest extends HibernateTestCase {
    private static final String OFFERING1 = "offering1";
    private static final String OFFERING2 = "offering2";
    private static final String OFFERING3 = "offering3";
    private static final String PROCEDURE1 = "procedure1";
    private static final String PROCEDURE2 = "procedure2";
    private static final String PROCEDURE3 = "procedure3";
    private static final String OBSPROP1 = "obsprop1";
    private static final String OBSPROP2 = "obsprop2";
    private static final String OBSPROP3 = "obsprop3";
    private static final String FEATURE3 = "feature3";
    private static final String RESULT_TEMPLATE = "result_template";
    private static final DateTime TIME1 = new DateTime("2013-07-18T00:00:00Z");
    private static final DateTime TIME2 = new DateTime("2013-07-18T01:00:00Z");
    private static final DateTime TIME3 = new DateTime("2013-07-18T02:00:00Z");
    private static final DateTime OBS_TIME = new DateTime("2013-07-18T03:00:00Z");
    private static final DateTime OBS_TIME_SP = new DateTime("2015-07-18T03:00:00Z");
    private static final DateTime OBS_TIME_PARAM = new DateTime("2015-07-18T04:00:00Z");
    private static final DateTime OBS_TIME_HEIGHT = new DateTime("2015-07-18T05:00:00Z");
    private static final DateTime OBS_TIME_DEPTH = new DateTime("2015-07-18T06:00:00Z");
    private static final Double VAL1 = 19.1;
    private static final Double VAL2 = 19.8;
    private static final Double VAL3 = 20.4;
    private static final Double OBS_VAL = 20.8;
    private static final String TOKEN_SEPARATOR = ",";
    private static final String DECIMAL_SEPARATOR = ".";
    private static final String BLOCK_SEPARATOR = "#";
    private static final String TEMP_UNIT = "Cel";
    private static final Geometry geometry = new GeometryFactory().createPoint(new Coordinate(52.7, 7.52));
    // om:parameter values
    private static final String BOOLEAN_PARAM_NAME = "booleanParamName";
    private static final boolean BOOLEAN_PARAM_VALUE = true;
    private static final String CATEGORY_PARAM_NAME = "categoryParamName";
    private static final String CATEGORY_PARAM_VALUE = "categoryParamValue";
    private static final String CATEGORY_PARAM_UNIT = "categoryParamUnit";
    private static final String COUNT_PARAM_NAME = "countParamName";
    private static final int COUNT_PARAM_VALUE = 123;
    private static final String QUANTITY_PARAM_NAME = "quantityParamName";
    private static final double QUANTITY_PARAM_VALUE = 12.3;
    private static final String QUANTITY_PARAM_UNIT = "m";
    private static final String TEXT_PARAM_NAME = "textParamName";
    private static final String TEXT_PARAM_VALUE = "textParamNValue";
    private static final double HEIGHT_DEPTH_VALUE = 10.0;
    private static final double HEIGHT_DEPTH_VALUE_2 = 20.0;
    private static final String HEIGHT_DEPTH_UNIT = "m";

    private EventBus serviceEventBus = new EventBus();


    /* FIXTURES */
    private final InsertSensorDAO insertSensorDAO = new InsertSensorDAO();
    private final DeleteSensorDAO deleteSensorDAO = new DeleteSensorDAO();
    private final InsertObservationDAO insertObservationDAO = new InsertObservationDAO();
    private final InsertResultTemplateDAO insertResultTemplateDAO = new InsertResultTemplateDAO();
    private final InsertResultDAO insertResultDAO = new InsertResultDAO();
    private final GetObservationDAO getObsDAO = new GetObservationDAO();
    private final SosInsertObservationOperatorV20 insertObservationOperatorv2 = new SosInsertObservationOperatorV20();
    private final EncoderRepository encoderRepository = new EncoderRepository();

    // optionally run these tests multiple times to expose intermittent faults
    // (use -DrepeatDaoTest=x)
    @Parameterized.Parameters
    public static List<Object[]> data() {
        int repeatDaoTest = 1;
        String repeatDaoTestStr = java.lang.System.getProperty("repeatDaoTest");

        if (repeatDaoTestStr != null) {
            repeatDaoTest = Integer.parseInt(repeatDaoTestStr);
        }
        return Arrays.asList(new Object[repeatDaoTest][0]);
    }

    @Before
    public void setUp() throws OwsExceptionReport, ConverterException, EncodingException, DecodingException {
        encoderRepository.init();
        Session session = getSession();
        insertSensor(PROCEDURE1, OFFERING1, OBSPROP1, null);
        insertSensor(PROCEDURE2, OFFERING2, OBSPROP2, PROCEDURE1);
        insertSensor(PROCEDURE3, OFFERING3, OBSPROP3, PROCEDURE2);
        insertResultTemplate(RESULT_TEMPLATE, PROCEDURE3, OFFERING3, OBSPROP3, FEATURE3, getSession());
        returnSession(session);
    }

    @After
    public void tearDown() throws OwsExceptionReport, InterruptedException {
        H2Configuration.truncate();
    }

    @AfterClass
    public static void cleanUp() {
        H2Configuration.recreate();
    }

    private void insertSensor(String procedure, String offering, String obsProp, String parentProcedure)
            throws OwsExceptionReport, EncodingException {
        InsertSensorRequest req = new InsertSensorRequest();
        req.setAssignedProcedureIdentifier(procedure);
        List<SosOffering> assignedOfferings = Lists.newLinkedList();
        assignedOfferings.add(new SosOffering(offering, offering));
        req.setObservableProperty(CollectionHelper.list(obsProp));
        req.setProcedureDescriptionFormat(SensorMLConstants.NS_SML);
        SosInsertionMetadata meta = new SosInsertionMetadata();
        meta.setObservationTypes(Sets.newHashSet(OmConstants.OBS_TYPE_MEASUREMENT));
        meta.setFeatureOfInterestTypes(Sets.newHashSet(SfConstants.SAMPLING_FEAT_TYPE_SF_SAMPLING_POINT));
        req.setMetadata(meta);
        System system = new System();
        system.setIdentifier(procedure);
        SosProcedureDescription<?> pd = new SosProcedureDescription<AbstractFeature>(system);
        if (parentProcedure != null) {
            pd.setParentProcedure(new ReferenceType(parentProcedure, parentProcedure));
            for (String hierarchyParentProc : getCache().getParentProcedures(parentProcedure, true, true)) {
                for (String parentProcOffering : getCache().getOfferingsForProcedure(hierarchyParentProc)) {
                    SosOffering sosOffering = new SosOffering(parentProcOffering, parentProcOffering);
                    sosOffering.setParentOfferingFlag(true);
                    assignedOfferings.add(sosOffering);
                }
            }
        }
        SystemDocument xbSystemDoc = SystemDocument.Factory.newInstance();
        xbSystemDoc.addNewSystem().set((XmlObject)encoderRepository.getEncoder(CodingHelper.getEncoderKey(SensorMLConstants.NS_SML, system)).encode(system));
        system.setXml(xbSystemDoc.xmlText());
        req.setProcedureDescription(pd);
        req.setAssignedOfferings(assignedOfferings);
        InsertSensorResponse resp = insertSensorDAO.insertSensor(req);
        this.serviceEventBus.submit(new SensorInsertion(req, resp));
    }

    @SuppressWarnings("unused")
    private void deleteSensor(String procedure) throws OwsExceptionReport {
        DeleteSensorRequest req = new DeleteSensorRequest();
        req.setProcedureIdentifier(procedure);
        DeleteSensorResponse resp = deleteSensorDAO.deleteSensor(req);
        this.serviceEventBus.submit(new SensorDeletion(req, resp));
    }

    private void insertResultTemplate(String identifier, String procedureId, String offeringId, String obsPropId,
            String featureId, Session session) throws EncodingException, ConverterException, OwsExceptionReport, DecodingException {
        InsertResultTemplateRequest req = new InsertResultTemplateRequest();
        req.setIdentifier(identifier);
        req.setObservationTemplate(getOmObsConst(procedureId, obsPropId, TEMP_UNIT, offeringId, featureId,
                OmConstants.OBS_TYPE_MEASUREMENT, session));

        SweTextEncoding textEncoding = new SweTextEncoding();
        textEncoding.setCollapseWhiteSpaces(false);
        textEncoding.setDecimalSeparator(DECIMAL_SEPARATOR);
        textEncoding.setTokenSeparator(TOKEN_SEPARATOR);
        textEncoding.setBlockSeparator(BLOCK_SEPARATOR);
        SosResultEncoding resultEncoding = new SosResultEncoding(textEncoding);
        TextEncodingDocument xbTextEncDoc = TextEncodingDocument.Factory.newInstance();
        xbTextEncDoc.addNewTextEncoding()
                .set((XmlObject) encoderRepository
                        .getEncoder(CodingHelper.getEncoderKey(SweConstants.NS_SWE_20, textEncoding))
                        .encode(textEncoding));
        resultEncoding.setXml(xbTextEncDoc.xmlText());
        req.setResultEncoding(resultEncoding);

        SweDataRecord dataRecord = new SweDataRecord();
        SweTime sweTime = new SweTime();
        sweTime.setUom(OmConstants.PHEN_UOM_ISO8601);
        sweTime.setDefinition(OmConstants.PHENOMENON_TIME);
        dataRecord.addField(new SweField("time", sweTime));
        SweQuantity airTemp = new SweQuantity();
        airTemp.setDefinition(obsPropId);
        airTemp.setUom(TEMP_UNIT);
        dataRecord.addField(new SweField("air_temperature", airTemp));
        SosResultStructure resultStructure = new SosResultStructure(dataRecord);
        DataRecordDocument xbDataRecordDoc = DataRecordDocument.Factory.newInstance();
        xbDataRecordDoc.addNewDataRecord()
                .set((XmlObject) encoderRepository
                        .getEncoder(CodingHelper.getEncoderKey(SweConstants.NS_SWE_20, dataRecord))
                        .encode(dataRecord));
        resultStructure.setXml(xbDataRecordDoc.xmlText());
        req.setResultStructure(resultStructure);
        InsertResultTemplateResponse resp = insertResultTemplateDAO.insertResultTemplate(req);
        this.serviceEventBus.submit(new ResultTemplateInsertion(req, resp));
    }

    private SosContentCache getCache() {
        return Configurator.getInstance().getCache();
    }

    private void updateCache() throws OwsExceptionReport {
        Configurator.getInstance().getCacheController().update();
    }

    private OmObservationConstellation getOmObsConst(String procedureId, String obsPropId, String unit,
            String offeringId, String featureId, String obsType, Session session) throws OwsExceptionReport,
            ConverterException {
        OmObservationConstellation obsConst = new OmObservationConstellation();
        DaoFactory daoFactory = new DaoFactory();
        daoFactory.setI18NDAORepository(new I18NDAORepository());


        ProcedureDAO dao = daoFactory.getProcedureDAO();
        Procedure procedure = dao.getProcedureForIdentifier(procedureId, session);
        OwsServiceProviderFactory serviceProviderFactory = Mockito.mock(OwsServiceProviderFactory.class);

        SosProcedureDescription<?> spd
                = new HibernateProcedureConverter(new HibernateProcedureCreationContext(null, null, null, null, null, null, null, null, null, null, null))
                        .createSosProcedureDescription(procedure, SensorMLConstants.NS_SML,
                        Sos2Constants.SERVICEVERSION, session);
        obsConst.setProcedure(spd);
        OmObservableProperty omObservableProperty = new OmObservableProperty(obsPropId);
        omObservableProperty.setUnit(unit);
        obsConst.setObservableProperty(omObservableProperty);
        obsConst.setFeatureOfInterest(new SamplingFeature(new CodeWithAuthority(featureId)));
        obsConst.setObservationType(obsType);
        if (!Strings.isNullOrEmpty(offeringId)) {
            Set<String> offerings = new HashSet<String>();
            offerings.add(offeringId);
            obsConst.setOfferings(offerings);
        }
        return obsConst;
    }
    
    private String makeResultValueString(List<DateTime> times, List<Double> values) {
        if (times.size() != values.size()) {
            throw new RuntimeException("times and values must be the same length (times: " + times.size()
                    + ", values: " + values.size() + ")");
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < times.size(); i++) {
            if (i > 0) {
                sb.append(BLOCK_SEPARATOR);
            }
            sb.append(times.get(i));
            sb.append(TOKEN_SEPARATOR);
            sb.append(values.get(i));
        }
        return sb.toString();
    }

    private void assertInsertionAftermathBeforeAndAfterCacheReload() throws OwsExceptionReport, InterruptedException {
        // check once for cache changes triggered by sos event
        assertInsertionAftermath();

        // run a cache update
        updateCache();

        // check again after cache is reloaded
        assertInsertionAftermath();
    }

    private void assertInsertionAftermath() throws OwsExceptionReport {
        // check observation types
//        assertThat(getCache().getObservationTypesForOffering(OFFERING1), contains(OmConstants.OBS_TYPE_MEASUREMENT));
//        assertThat(getCache().getObservationTypesForOffering(OFFERING2), contains(OmConstants.OBS_TYPE_MEASUREMENT));
        assertThat(getCache().getObservationTypesForOffering(OFFERING3), contains(OmConstants.OBS_TYPE_MEASUREMENT));

        // check offerings for procedure
        assertThat(getCache().getOfferingsForProcedure(PROCEDURE1), contains(OFFERING1));
        assertThat(getCache().getOfferingsForProcedure(PROCEDURE2), containsInAnyOrder(OFFERING1, OFFERING2));
        assertThat(getCache().getOfferingsForProcedure(PROCEDURE3),
                containsInAnyOrder(OFFERING1, OFFERING2, OFFERING3));

        // check procedures and hidden child procedures for offering
        assertThat(getCache().getProceduresForOffering(OFFERING1), contains(PROCEDURE1));
        assertThat(getCache().getHiddenChildProceduresForOffering(OFFERING1),
                containsInAnyOrder(PROCEDURE2, PROCEDURE3));

        assertThat(getCache().getProceduresForOffering(OFFERING2), contains(PROCEDURE2));
        assertThat(getCache().getHiddenChildProceduresForOffering(OFFERING2), contains(PROCEDURE3));

        assertThat(getCache().getProceduresForOffering(OFFERING3), contains(PROCEDURE3));
        assertThat(getCache().getHiddenChildProceduresForOffering(OFFERING3), empty());

        // check allowed observation types for offering
        assertThat(getCache().getAllowedObservationTypesForOffering(OFFERING1),
                contains(OmConstants.OBS_TYPE_MEASUREMENT));
        assertThat(getCache().getAllowedObservationTypesForOffering(OFFERING2),
                contains(OmConstants.OBS_TYPE_MEASUREMENT));
        assertThat(getCache().getAllowedObservationTypesForOffering(OFFERING3),
                contains(OmConstants.OBS_TYPE_MEASUREMENT));

        // check parent procedures
        assertThat(getCache().getParentProcedures(PROCEDURE1, true, false), empty());
        assertThat(getCache().getParentProcedures(PROCEDURE2, true, false), contains(PROCEDURE1));
        assertThat(getCache().getParentProcedures(PROCEDURE3, true, false), containsInAnyOrder(PROCEDURE1, PROCEDURE2));

        // check child procedures
        assertThat(getCache().getChildProcedures(PROCEDURE1, true, false), containsInAnyOrder(PROCEDURE2, PROCEDURE3));
        assertThat(getCache().getChildProcedures(PROCEDURE2, true, false), contains(PROCEDURE3));
        assertThat(getCache().getChildProcedures(PROCEDURE3, true, false), empty());

        // check features of interest for offering
        // TODO add geometries to features, check bounds, etc
        // TODO investigate these, getting guid back instead of assigned
        // identifier
        // assertThat(getCache().getFeaturesOfInterestForOffering(OFFERING1),
        // contains(FEATURE3));
        // assertThat(getCache().getFeaturesOfInterestForOffering(OFFERING2),
        // contains(FEATURE3));
        // assertThat(getCache().getFeaturesOfInterestForOffering(OFFERING3),
        // contains(FEATURE3));

        // check obsprops for offering
        assertThat(getCache().getObservablePropertiesForOffering(OFFERING1),
                containsInAnyOrder(OBSPROP1, OBSPROP2, OBSPROP3));
        assertThat(getCache().getObservablePropertiesForOffering(OFFERING2), containsInAnyOrder(OBSPROP2, OBSPROP3));
        assertThat(getCache().getObservablePropertiesForOffering(OFFERING3), contains(OBSPROP3));

        // check offering for obsprops
        assertThat(getCache().getOfferingsForObservableProperty(OBSPROP1), contains(OFFERING1));
        assertThat(getCache().getOfferingsForObservableProperty(OBSPROP2), containsInAnyOrder(OFFERING1, OFFERING2));
        assertThat(getCache().getOfferingsForObservableProperty(OBSPROP3),
                containsInAnyOrder(OFFERING1, OFFERING2, OFFERING3));

        // check obsprops for procedure
        // TODO child procedure obsprops are not currently set for parents.
        // should they be?
        // assertThat(getCache().getObservablePropertiesForProcedure(PROCEDURE1),
        // containsInAnyOrder(OBSPROP1, OBSPROP2, OBSPROP3));
        // assertThat(getCache().getObservablePropertiesForProcedure(PROCEDURE2),
        // containsInAnyOrder(OBSPROP2, OBSPROP3));
        assertThat(getCache().getObservablePropertiesForProcedure(PROCEDURE3), contains(OBSPROP3));

        // check procedures for obsprop
        // TODO child procedure obsprops are not currently set for parents.
        // should they be?
        assertThat(getCache().getProceduresForObservableProperty(OBSPROP1), contains(PROCEDURE1));
        // assertThat(getCache().getProceduresForObservableProperty(OBSPROP2),
        // containsInAnyOrder(PROCEDURE1, PROCEDURE2));
        // assertThat(getCache().getProceduresForObservableProperty(OBSPROP3),
        // containsInAnyOrder(PROCEDURE1, PROCEDURE2, PROCEDURE3));

        // check procedures for feature
        // TODO child procedure features are not currently set for parents.
        // should they be?
        // assertThat(getCache().getProceduresForFeatureOfInterest(FEATURE3),
        // containsInAnyOrder(PROCEDURE1, PROCEDURE2, PROCEDURE3));
    }

    @Test
    public void testCacheContents() throws OwsExceptionReport {
        assertThat(getCache().getProcedures(), containsInAnyOrder(PROCEDURE1, PROCEDURE2, PROCEDURE3));
        assertThat(getCache().getOfferings(), containsInAnyOrder(OFFERING1, OFFERING2, OFFERING3));
        assertThat(getCache().getObservableProperties(), containsInAnyOrder(OBSPROP1, OBSPROP2, OBSPROP3));
        assertThat(getCache().getParentProcedures(PROCEDURE3, true, false), containsInAnyOrder(PROCEDURE1, PROCEDURE2));
        assertThat(getCache().getResultTemplatesForOffering(OFFERING3).size(), is(1));
    }

    @Test
    public void testInsertObservation() throws OwsExceptionReport, InterruptedException, ConverterException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME));
        obsVal.setValue(new QuantityValue(Double.valueOf(OBS_VAL), TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();

        // TODO requests for the parent procedures fail?
        // checkObservation(OFFERING1, PROCEDURE1, OBSPROP3, OBS_TIME,
        // PROCEDURE3, OBSPROP3, FEATURE3,
        // OBS_VAL, TEMP_UNIT);
        // checkObservation(OFFERING2, PROCEDURE2, OBSPROP3, OBS_TIME,
        // PROCEDURE3, OBSPROP3, FEATURE3,
        // OBS_VAL, TEMP_UNIT);
        checkObservation(Lists.newArrayList(OFFERING1, OFFERING2, OFFERING3), PROCEDURE3, OBSPROP3, OBS_TIME, PROCEDURE3, OBSPROP3, FEATURE3, OBS_VAL, TEMP_UNIT);
        checkObservation(Lists.newArrayList(OFFERING2, OFFERING3), PROCEDURE3, OBSPROP3, OBS_TIME, PROCEDURE3, OBSPROP3, FEATURE3, OBS_VAL, TEMP_UNIT);
        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, OBS_TIME, PROCEDURE3, OBSPROP3, FEATURE3, OBS_VAL, TEMP_UNIT);
    }

    /**
     * Check results of an InsertObservation request with
     * SplitDataArrayIntoObservations
     */
    // TODO should this test live in another module, since it involves
    // transactional-v20?
    // however, it also tests functionality.
    @Test
    public void testInsertObservationWithSplit() throws OwsExceptionReport, InterruptedException, ConverterException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setService(SosConstants.SOS);
        req.setVersion(Sos2Constants.SERVICEVERSION);

        SwesExtension<SweBoolean> splitExt = new SwesExtension<>();
        splitExt.setDefinition(Sos2Constants.Extensions.SplitDataArrayIntoObservations.name());
        splitExt.setValue(new SweBoolean().setValue(Boolean.TRUE));
        SwesExtensions swesExtensions = new SwesExtensions();
        swesExtensions.addExtension(splitExt);
        req.setExtensions(swesExtensions);

        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(IndeterminateValue.TEMPLATE));
        SweDataArrayValue sweDataArrayValue = new SweDataArrayValue();
        SweDataArray sweDataArray = new SweDataArray();
        sweDataArray.setElementCount(new SweCount().setValue(3));
        SweDataRecord sweDataRecord = new SweDataRecord();

        SweTime time = new SweTime();
        time.setDefinition(OmConstants.PHENOMENON_TIME);
        time.setUom(OmConstants.PHEN_UOM_ISO8601);
        SweField timeField = new SweField(OmConstants.PHENOMENON_TIME_NAME, time);
        sweDataRecord.addField(timeField);

        SweQuantity temp = new SweQuantity();
        temp.setDefinition(OBSPROP3);
        temp.setUom(TEMP_UNIT);
        SweField tempField = new SweField(OmConstants.EN_OBSERVED_PROPERTY, temp);
        sweDataRecord.addField(tempField);

        sweDataArray.setElementType(sweDataRecord);

        SweTextEncoding sweTextEncoding = new SweTextEncoding();
        sweTextEncoding.setBlockSeparator("#");
        sweTextEncoding.setDecimalSeparator(".");
        sweTextEncoding.setTokenSeparator("@");
        sweDataArray.setEncoding(sweTextEncoding);

        // add values
        sweDataArray.add(CollectionHelper.list(TIME1.toString(), Double.toString(VAL1)));
        sweDataArray.add(CollectionHelper.list(TIME2.toString(), Double.toString(VAL2)));
        sweDataArray.add(CollectionHelper.list(TIME3.toString(), Double.toString(VAL3)));

        sweDataArrayValue.setValue(sweDataArray);

        SingleObservationValue<SweDataArray> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(IndeterminateValue.TEMPLATE));
        obsVal.setValue(sweDataArrayValue);
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs))
            .setRequestContext(new OwsServiceRequestContext());
        insertObservationOperatorv2.receiveRequest(req);
        assertInsertionAftermathBeforeAndAfterCacheReload();

        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, TIME1, PROCEDURE3, OBSPROP3, FEATURE3, VAL1, TEMP_UNIT);
        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, TIME2, PROCEDURE3, OBSPROP3, FEATURE3, VAL2, TEMP_UNIT);
        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, TIME3, PROCEDURE3, OBSPROP3, FEATURE3, VAL3, TEMP_UNIT);
    }

    @Test
    public void testInsertResult() throws OwsExceptionReport, InterruptedException {
        InsertResultRequest req = new InsertResultRequest();
        req.setTemplateIdentifier(RESULT_TEMPLATE);
        req.setResultValues(makeResultValueString(CollectionHelper.list(TIME1, TIME2, TIME3),
                CollectionHelper.list(VAL1, VAL2, VAL3)));
        InsertResultResponse resp = insertResultDAO.insertResult(req);
        this.serviceEventBus.submit(new ResultInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();

        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, TIME1, PROCEDURE3, OBSPROP3, FEATURE3, VAL1, TEMP_UNIT);
        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, TIME2, PROCEDURE3, OBSPROP3, FEATURE3, VAL2, TEMP_UNIT);
        checkObservation(OFFERING3, PROCEDURE3, OBSPROP3, TIME3, PROCEDURE3, OBSPROP3, FEATURE3, VAL3, TEMP_UNIT);
    }

    @Test
    public void testInsertObservationWithSamplingGeometry() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_SP));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_SP));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        obs.addParameter(createSamplingGeometry());
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        checkSamplingGeometry(OFFERING3, PROCEDURE3, OBSPROP3, FEATURE3, OBS_TIME_SP);
    }

    private NamedValue<?> createSamplingGeometry() {
        final NamedValue<Geometry> namedValue = new NamedValue<>();
        final ReferenceType referenceType = new ReferenceType(OmConstants.PARAM_NAME_SAMPLING_GEOMETRY);
        namedValue.setName(referenceType);
        // TODO add lat/long version
        namedValue.setValue(new org.n52.shetland.ogc.om.values.GeometryValue(geometry));
        return namedValue;
    }


    private void checkSamplingGeometry(String offering, String procedure, String obsprop, String feature, DateTime time) throws OwsExceptionReport {
        GetObservationRequest getObsReq = createDefaultGetObservationRequest(offering, procedure, obsprop, time, feature);
        GetObservationResponse getObsResponse = getObsDAO.getObservation(getObsReq);
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse.getObservationCollection().hasNext(), is(true));
        OmObservation omObservation = getObsResponse.getObservationCollection().next();
        if (omObservation.getValue() instanceof StreamingValue) {
            assertThat(((StreamingValue)omObservation.getValue()).hasNext(), is(true));
            omObservation = ((StreamingValue)omObservation.getValue()).next();
        }
        if (omObservation != null) {
            assertThat(omObservation.isSetParameter(), is(true));
            assertThat(omObservation.isSetSpatialFilteringProfileParameter(), is(true));
            checkNamedValue(omObservation.getSpatialFilteringProfileParameter(), OmConstants.PARAM_NAME_SAMPLING_GEOMETRY, geometry, null);
        }
    }

    @Test
    public void testInsertObservationWithOmParameter() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_PARAM));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_PARAM));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        addParameter(obs);
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        checkOmParameter(OFFERING3, PROCEDURE3, OBSPROP3, FEATURE3, OBS_TIME_PARAM);
    }

    private void addParameter(OmObservation obs) {
       obs.addParameter(createBooleanParameter(BOOLEAN_PARAM_NAME, BOOLEAN_PARAM_VALUE));
       obs.addParameter(createCategoryParameter(CATEGORY_PARAM_NAME, CATEGORY_PARAM_VALUE, CATEGORY_PARAM_UNIT));
       obs.addParameter(createCountParameter(COUNT_PARAM_NAME, COUNT_PARAM_VALUE));
       obs.addParameter(createQuantityParameter(QUANTITY_PARAM_NAME, QUANTITY_PARAM_VALUE, QUANTITY_PARAM_UNIT));
       obs.addParameter(createTextParameter(TEXT_PARAM_NAME, TEXT_PARAM_VALUE));
    }

    private void checkOmParameter(String offering, String procedure, String obsprop, String feature,
            DateTime obsTimeParam) throws OwsExceptionReport {
        GetObservationRequest getObsReq = createDefaultGetObservationRequest(offering, procedure, obsprop, obsTimeParam, feature);
        GetObservationResponse getObsResponse = getObsDAO.getObservation(getObsReq);
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse.getObservationCollection().hasNext(), is(true));
        OmObservation omObservation = getObsResponse.getObservationCollection().next();
        if (omObservation.getValue() instanceof StreamingValue) {
            assertThat(((StreamingValue)omObservation.getValue()).hasNext(), is(true));
            omObservation = ((StreamingValue)omObservation.getValue()).next();
        }
        if (omObservation != null) {
            assertThat(omObservation.isSetParameter(), is(true));
            assertThat(omObservation.getParameter().size(), is(5));
            for (NamedValue<?> namedValue : omObservation.getParameter()) {
                assertThat(namedValue.isSetName(), is(true));
                assertThat(namedValue.getName().isSetHref(), is(true));
                if (BOOLEAN_PARAM_NAME.equals(namedValue.getName().getHref())) {
                    checkNamedValue(namedValue, BOOLEAN_PARAM_NAME, BOOLEAN_PARAM_VALUE, null);
                } else if (CATEGORY_PARAM_NAME.equals(namedValue.getName().getHref())) {
                    checkNamedValue(namedValue, CATEGORY_PARAM_NAME, CATEGORY_PARAM_VALUE, CATEGORY_PARAM_UNIT);
                } else if (COUNT_PARAM_NAME.equals(namedValue.getName().getHref())) {
                    checkNamedValue(namedValue, COUNT_PARAM_NAME, COUNT_PARAM_VALUE, null);
                } else if (QUANTITY_PARAM_NAME.equals(namedValue.getName().getHref())) {
                    checkNamedValue(namedValue, QUANTITY_PARAM_NAME, QUANTITY_PARAM_VALUE, QUANTITY_PARAM_UNIT);
                } else if (TEXT_PARAM_NAME.equals(namedValue.getName().getHref())) {
                    checkNamedValue(namedValue, TEXT_PARAM_NAME, TEXT_PARAM_VALUE, null);
                }
            }
        }
    }

    @Test
    public void testInsertObservationWithHeightParameter() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_HEIGHT));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_HEIGHT));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        obs.addParameter(createHeight(HEIGHT_DEPTH_VALUE));
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        checkHeightParameter(OFFERING3, PROCEDURE3, OBSPROP3, FEATURE3, OBS_TIME_HEIGHT);
    }

    private void checkHeightParameter(String offering, String procedure, String obsprop, String feature, DateTime time) throws OwsExceptionReport {
        GetObservationRequest getObsReq = createDefaultGetObservationRequest(offering, procedure, obsprop, time, feature);
        GetObservationResponse getObsResponse = getObsDAO.getObservation(getObsReq);
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse.getObservationCollection().hasNext(), is(true));
        OmObservation omObservation = getObsResponse.getObservationCollection().next();
        if (omObservation.getValue() instanceof StreamingValue) {
            assertThat(((StreamingValue)omObservation.getValue()).hasNext(), is(true));
            omObservation = ((StreamingValue)omObservation.getValue()).next();
        }
        if (omObservation != null) {
            assertThat(omObservation.isSetParameter(), is(true));
            assertThat(omObservation.isSetHeightParameter(), is(true));
            checkNamedValue(omObservation.getHeightParameter(), OmConstants.PARAMETER_NAME_HEIGHT_URL, HEIGHT_DEPTH_VALUE, HEIGHT_DEPTH_UNIT);
        }
    }

    @Test
    public void testInsertObservationWithDepthParameter() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_DEPTH));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_DEPTH));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        obs.addParameter(createDepth(HEIGHT_DEPTH_VALUE));
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        checkDepthParameter(OFFERING3, PROCEDURE3, OBSPROP3, FEATURE3, OBS_TIME_DEPTH);
    }

    private void checkDepthParameter(String offering, String procedure, String obsprop, String feature, DateTime time) throws OwsExceptionReport {
        GetObservationRequest getObsReq = createDefaultGetObservationRequest(offering, procedure, obsprop, time, feature);
        GetObservationResponse getObsResponse = getObsDAO.getObservation(getObsReq);
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse.getObservationCollection().hasNext(), is(true));
        OmObservation omObservation = getObsResponse.getObservationCollection().next();
        if (omObservation.getValue() instanceof StreamingValue) {
            assertThat(((StreamingValue)omObservation.getValue()).hasNext(), is(true));
            omObservation = ((StreamingValue)omObservation.getValue()).next();
        }
        if (omObservation != null) {
            assertThat(omObservation.isSetParameter(), is(true));
            assertThat(omObservation.isSetDepthParameter(), is(true));
            checkNamedValue(omObservation.getDepthParameter(), OmConstants.PARAMETER_NAME_DEPTH_URL, HEIGHT_DEPTH_VALUE, HEIGHT_DEPTH_UNIT);
        }
    }

    @Test(expected=OwsExceptionReport.class)
    public void testInsertDuplicateObservation() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_DEPTH));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_DEPTH));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        InsertObservationResponse resp2 = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp2));
        assertInsertionAftermathBeforeAndAfterCacheReload();
    }


    @Test(expected=OwsExceptionReport.class)
    public void testInsertDuplicateObservationWithDepthParameter() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_DEPTH));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_DEPTH));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        obs.addParameter(createDepth(HEIGHT_DEPTH_VALUE));
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        InsertObservationResponse resp2 = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp2));
        assertInsertionAftermathBeforeAndAfterCacheReload();
    }

    @Test(expected=OwsExceptionReport.class)
    public void testInsertDuplicateObservationWithHeightParameter() throws OwsExceptionReport, ConverterException, InterruptedException {
        InsertObservationRequest req = new InsertObservationRequest();
        req.setAssignedSensorId(PROCEDURE3);
        req.setOfferings(Lists.newArrayList(OFFERING3));
        OmObservation obs = new OmObservation();

        Session session = getSession();
        obs.setObservationConstellation(getOmObsConst(PROCEDURE3, OBSPROP3, TEMP_UNIT, OFFERING3, FEATURE3,
                OmConstants.OBS_TYPE_MEASUREMENT, session));
        returnSession(session);

        obs.setResultTime(new TimeInstant(OBS_TIME_HEIGHT));
        SingleObservationValue<Double> obsVal = new SingleObservationValue<>();
        obsVal.setPhenomenonTime(new TimeInstant(OBS_TIME_HEIGHT));
        obsVal.setValue(new QuantityValue(OBS_VAL, TEMP_UNIT));
        obs.setValue(obsVal);
        req.setObservation(Lists.newArrayList(obs));
        obs.addParameter(createHeight(HEIGHT_DEPTH_VALUE));
        InsertObservationResponse resp = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp));
        assertInsertionAftermathBeforeAndAfterCacheReload();
        InsertObservationResponse resp2 = insertObservationDAO.insertObservation(req);
        this.serviceEventBus.submit(new ObservationInsertion(req, resp2));
        assertInsertionAftermathBeforeAndAfterCacheReload();
    }

    private NamedValue<?> createHeight(double value) {
        return createQuantityParameter(OmConstants.PARAMETER_NAME_HEIGHT_URL, value, HEIGHT_DEPTH_UNIT);
    }

    private NamedValue<?> createDepth(double value) {
        return createQuantityParameter(OmConstants.PARAMETER_NAME_DEPTH_URL, value, HEIGHT_DEPTH_UNIT);
    }
    
    private GetObservationRequest createDefaultGetObservationRequest(String reqOffering, String reqProcedure, String reqObsProp, DateTime time,
            String obsFeature) {
        return createDefaultGetObservationRequest(Lists.newArrayList(reqOffering), reqProcedure, reqObsProp, time, obsFeature);
    }

    private GetObservationRequest createDefaultGetObservationRequest(List<String> reqOffering, String reqProcedure, String reqObsProp, DateTime time,
            String obsFeature) {
        GetObservationRequest getObsReq = new GetObservationRequest();
        getObsReq.setOfferings(reqOffering);
        getObsReq.setProcedures(CollectionHelper.list(reqProcedure));
        getObsReq.setObservedProperties(CollectionHelper.list(reqObsProp));
        getObsReq.setFeatureIdentifiers(CollectionHelper.list(obsFeature));
        getObsReq.setResponseFormat(OmConstants.NS_OM_2);
        TemporalFilter tempFilter =
                new TemporalFilter(FilterConstants.TimeOperator.TM_Equals, new TimeInstant(time),
                        TemporalRestrictions.PHENOMENON_TIME_VALUE_REFERENCE);
        getObsReq.setTemporalFilters(CollectionHelper.list(tempFilter));
        getObsReq.setService(SosConstants.SOS);
        getObsReq.setVersion(Sos2Constants.SERVICEVERSION);
        return getObsReq;
    }
    
    private void checkObservation(String reqOffering, String reqProcedure, String reqObsProp, DateTime time,
            String obsProcedure, String obsObsProp, String obsFeature, Double obsVal, String obsUnit)
            throws OwsExceptionReport {
        checkObservation(Lists.newArrayList(reqOffering), reqProcedure, reqObsProp, time,
                obsProcedure, obsObsProp, obsFeature, obsVal, obsUnit);
    }

    private void checkObservation(List<String> reqOffering, String reqProcedure, String reqObsProp, DateTime time,
            String obsProcedure, String obsObsProp, String obsFeature, Double obsVal, String obsUnit)
            throws OwsExceptionReport {
        GetObservationRequest getObsReq = createDefaultGetObservationRequest(reqOffering, reqProcedure, reqObsProp, time,
            obsFeature);
        GetObservationResponse getObsResponse = getObsDAO.getObservation(getObsReq);
        assertThat(getObsResponse, notNullValue());
        assertThat(getObsResponse.getObservationCollection().hasNext(), is(true));
        OmObservation omObservation = getObsResponse.getObservationCollection().next();
        if (omObservation.getValue() instanceof StreamingValue) {
            assertThat(((StreamingValue)omObservation.getValue()).hasNext(), is(true));
            omObservation = ((StreamingValue)omObservation.getValue()).next();
            assertThat(omObservation, notNullValue());
            assertThat(omObservation.getObservationConstellation(), notNullValue());
            OmObservationConstellation obsConst = omObservation.getObservationConstellation();
            assertThat(obsConst.getProcedure().getIdentifier(), is(obsProcedure));
            assertThat(obsConst.getObservableProperty().getIdentifier(), is(obsObsProp));

            // TODO this fails
            // assertThat(obsConst.getFeatureOfInterest().getIdentifier().getValue(),
            // is(obsFeature));

            assertThat(omObservation.getValue(), notNullValue());
            ObservationValue<?> value = omObservation.getValue();
            assertThat(value.getValue(), instanceOf(QuantityValue.class));
            assertThat(value.getPhenomenonTime(), instanceOf(TimeInstant.class));
            TimeInstant timeInstant = (TimeInstant) value.getPhenomenonTime();
            assertThat(timeInstant.getValue().toDate(), is(time.toDate()));
            QuantityValue quantityValue = (QuantityValue) value.getValue();
            assertThat(quantityValue.getValue(), is(obsVal));
            assertThat(quantityValue.getUnit(), is(obsUnit));
        } else {
            assertThat(omObservation.getObservationConstellation(), notNullValue());
            OmObservationConstellation obsConst = omObservation.getObservationConstellation();
            assertThat(obsConst.getProcedure().getIdentifier(), is(obsProcedure));
            assertThat(obsConst.getObservableProperty().getIdentifier(), is(obsObsProp));

            // TODO this fails
            // assertThat(obsConst.getFeatureOfInterest().getIdentifier().getValue(),
            // is(obsFeature));

            assertThat(omObservation.getValue(), notNullValue());
            ObservationValue<?> value = omObservation.getValue();
            assertThat(value.getValue(), instanceOf(QuantityValue.class));
            assertThat(value.getPhenomenonTime(), instanceOf(TimeInstant.class));
            TimeInstant timeInstant = (TimeInstant) value.getPhenomenonTime();
            assertThat(timeInstant.getValue().toDate(), is(time.toDate()));
            QuantityValue quantityValue = (QuantityValue) value.getValue();
            assertThat(quantityValue.getValue(), is(obsVal));
            assertThat(quantityValue.getUnit(), is(obsUnit));
        }
    }

    private void checkNamedValue(NamedValue<?> namedValue, String name, Object value, String unit) {
        assertThat(namedValue.isSetName(), is(true));
        assertThat(namedValue.getName().isSetHref(), is(true));
        assertThat(namedValue.getName().getHref(), is(name));
        assertThat(namedValue.isSetValue(), is(true));
        assertThat(namedValue.getValue().isSetValue(), is(true));
        assertThat(namedValue.getValue().getValue(), is(value));
        if (!Strings.isNullOrEmpty(unit)) {
            assertThat(namedValue.getValue().isSetUnit(), is(true));
            assertThat(namedValue.getValue().getUnit(), is(unit));
        }
    }

    private NamedValue<?> createBooleanParameter(String name, boolean value) {
        final NamedValue<Boolean> namedValue = new NamedValue<>();
        final ReferenceType referenceType = new ReferenceType(name);
        namedValue.setName(referenceType);
        namedValue.setValue(new org.n52.shetland.ogc.om.values.BooleanValue(value));
        return namedValue;
    }

    private NamedValue<?> createCategoryParameter(String name, String value, String unit) {
        final NamedValue<String> namedValue = new NamedValue<>();
        final ReferenceType referenceType = new ReferenceType(name);
        namedValue.setName(referenceType);
        namedValue.setValue(new org.n52.shetland.ogc.om.values.CategoryValue(value, unit));
        return namedValue;
    }

    private NamedValue<?> createCountParameter(String name, int value) {
        final NamedValue<Integer> namedValue = new NamedValue<>();
        final ReferenceType referenceType = new ReferenceType(name);
        namedValue.setName(referenceType);
        namedValue.setValue(new org.n52.shetland.ogc.om.values.CountValue(value));
        return namedValue;
    }

    private NamedValue<?> createQuantityParameter(String name, double value, String unit) {
        final NamedValue<Double> namedValue = new NamedValue<>();
        final ReferenceType referenceType = new ReferenceType(name);
        namedValue.setName(referenceType);
        namedValue.setValue(new org.n52.shetland.ogc.om.values.QuantityValue(value, unit));
        return namedValue;
    }

    private NamedValue<?> createTextParameter(String name, String value) {
        final NamedValue<String> namedValue = new NamedValue<>();
        final ReferenceType referenceType = new ReferenceType(name);
        namedValue.setName(referenceType);
        namedValue.setValue(new org.n52.shetland.ogc.om.values.TextValue(value));
        return namedValue;
    }
}
