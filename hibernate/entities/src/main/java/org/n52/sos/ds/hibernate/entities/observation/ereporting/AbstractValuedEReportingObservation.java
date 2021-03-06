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
package org.n52.sos.ds.hibernate.entities.observation.ereporting;

import org.hibernate.Session;
import org.n52.shetland.aqd.AqdConstants;
import org.n52.shetland.aqd.ReportObligationType;
import org.n52.shetland.aqd.ReportObligations;
import org.n52.shetland.ogc.gml.time.Time;
import org.n52.shetland.ogc.gml.time.TimeInstant;
import org.n52.shetland.ogc.gml.time.TimePeriod;
import org.n52.shetland.ogc.om.OmConstants;
import org.n52.shetland.ogc.om.OmObservation;
import org.n52.shetland.ogc.om.values.Value;
import org.n52.shetland.ogc.ows.exception.OwsExceptionReport;
import org.n52.shetland.ogc.ows.extension.Extensions;
import org.n52.shetland.ogc.swe.SweDataArray;
import org.n52.shetland.util.DateTimeHelper;
import org.n52.sos.ds.hibernate.dao.ereporting.EReportingQualityDAO;
import org.n52.sos.ds.hibernate.entities.ereporting.EReportingQuality;
import org.n52.sos.ds.hibernate.entities.observation.ereporting.HibernateEReportingRelations.EReportingValues;
import org.n52.sos.ds.hibernate.entities.observation.series.AbstractValuedSeriesObservation;
import org.n52.sos.ds.hibernate.util.observation.EReportingHelper;

public abstract class AbstractValuedEReportingObservation<T>
        extends AbstractValuedSeriesObservation<T>
        implements ValuedEReportingObservation<T>, EReportingValues {

    private static final long serialVersionUID = 996063222630981539L;
    private Integer validation = EReportingValues.DEFAULT_VALIDATION;
    private Integer verification = EReportingValues.DEFAULT_VERIFICATION;
    private String primaryObservation = EReportingValues.DEFAULT_PRIMARY_OBSERVATION;
    private Boolean timeCoverageFlag;
    private Boolean dataCaptureFlag;
    private Double dataCapture;
    private Double uncertaintyEstimation;

    @Override
    public EReportingSeries getEReportingSeries() {
        return hasEReportingSeries() ? (EReportingSeries) getSeries() : null;
    }

    @Override
    public void setEReportingSeries(EReportingSeries series) {
        setSeries(series);
    }

    @Override
    public Integer getVerification() {
        return verification;
    }

    @Override
    public void setVerification(Integer verification) {
        this.verification = verification;
    }

    @Override
    public Integer getValidation() {
        return validation;
    }

    @Override
    public void setValidation(Integer validation) {
        this.validation = validation;
    }

    @Override
    public String getPrimaryObservation() {
        return primaryObservation;
    }

    @Override
    public void setPrimaryObservation(String primaryObservation) {
        this.primaryObservation = primaryObservation;
    }

    @Override
    public Boolean getDataCaptureFlag() {
        return this.dataCaptureFlag;
    }

    @Override
    public void setDataCaptureFlag(Boolean dataCaptureFlag) {
        this.dataCaptureFlag = dataCaptureFlag;
    }

    @Override
    public Double getDataCapture() {
        return this.dataCapture;
    }

    @Override
    public void setDataCapture(Double dataCapture) {
        this.dataCapture = dataCapture;
    }

    @Override
    public Boolean getTimeCoverageFlag() {
        return this.timeCoverageFlag;
    }

    @Override
    public void setTimeCoverageFlag(Boolean timeCoverageFlag) {
        this.timeCoverageFlag = timeCoverageFlag;
    }

    @Override
    public Double getUncertaintyEstimation() {
        return this.uncertaintyEstimation;
    }

    @Override
    public void setUncertaintyEstimation(Double uncertaintyEstimation) {
        this.uncertaintyEstimation = uncertaintyEstimation;
    }

    @Override
    public OmObservation mergeValueToObservation(OmObservation observation, String responseFormat)
            throws OwsExceptionReport {
        if (checkResponseFormat(responseFormat)) {
            if (!observation.isSetValue()) {
                addValuesToObservation(observation, responseFormat);
            } else {
                checkTime(observation);
                EReportingHelper.mergeValues((SweDataArray) observation.getValue().getValue().getValue(),
                        EReportingHelper.createSweDataArray(observation, this));
            }
            if (!OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION.equals(observation.getObservationConstellation()
                    .getObservationType())) {
                observation.getObservationConstellation().setObservationType(
                        OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION);
            }

        } else {
            super.mergeValueToObservation(observation, responseFormat);
        }
        return observation;
    }

    private void checkTime(OmObservation observation) {
        if (observation.isSetValue()) {
            Time obsPhenTime = observation.getValue().getPhenomenonTime();
            Time valuePhenTime = createPhenomenonTime();
            if (obsPhenTime != null) {
                TimePeriod timePeriod;
                if (obsPhenTime instanceof TimePeriod) {
                    timePeriod = (TimePeriod) obsPhenTime;
                } else {
                    timePeriod = new TimePeriod();
                    timePeriod.extendToContain(obsPhenTime);
                }
                timePeriod.extendToContain(valuePhenTime);
                observation.getValue().setPhenomenonTime(timePeriod);
            } else {
                observation.getValue().setPhenomenonTime(valuePhenTime);
            }
        }
        TimeInstant rt = createResutlTime(getResultTime());
        if (observation.getResultTime().getValue().isBefore(rt.getValue())) {
            observation.setResultTime(rt);
        }
        if (isSetValidTime()) {
            TimePeriod vt = createValidTime(getValidTimeStart(), getValidTimeEnd());
            if (observation.isSetValidTime()) {
                observation.getValidTime().extendToContain(vt);
            } else {
                observation.setValidTime(vt);
            }
        }
    }

    @Override
    public void addValueSpecificDataToObservation(OmObservation observation, Session session, Extensions extensions)
            throws OwsExceptionReport {
        if (ReportObligations.hasFlow(extensions)) {
            ReportObligationType flow = ReportObligations.getFlow(extensions);
            if (ReportObligationType.E1A.equals(flow) || ReportObligationType.E1B.equals(flow)) {
                int year = DateTimeHelper.makeDateTime(getPhenomenonTimeStart()).getYear();
                EReportingQuality eReportingQuality =
                        new EReportingQualityDAO().getEReportingQuality(getSeries().getSeriesId(), year,
                                getPrimaryObservation(), session);
                if (eReportingQuality != null) {
                    observation.setResultQuality(EReportingHelper.getGmdDomainConsistency(eReportingQuality, true));
                } else {
                    observation.setResultQuality(EReportingHelper.getGmdDomainConsistency(new EReportingQuality(), true));
                }
            }
        }
    }

    @Override
    public void addObservationValueToObservation(OmObservation observation, Value<?> value, String responseFormat)
            throws OwsExceptionReport {
        if (checkResponseFormat(responseFormat)) {
            if (!OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION.equals(observation.getObservationConstellation()
                    .getObservationType())) {
                observation.getObservationConstellation().setObservationType(
                        OmConstants.OBS_TYPE_SWE_ARRAY_OBSERVATION);
            }
            observation.setValue(EReportingHelper.createSweDataArrayValue(observation, this));
        } else {
            super.addObservationValueToObservation(observation, value, responseFormat);
        }

    }

    private boolean checkResponseFormat(String responseFormat) {
        return AqdConstants.NS_AQD.equals(responseFormat);
    }

    @Override
    public String getDiscriminator() {
        return getPrimaryObservation();
    }

}
