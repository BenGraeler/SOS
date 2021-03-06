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

import org.n52.sos.ds.hibernate.entities.observation.ereporting.HibernateEReportingRelations.EReportingValues;
import org.n52.sos.ds.hibernate.entities.observation.series.AbstractSeriesObservation;

public abstract class AbstractEReportingObservation<T>
        extends AbstractSeriesObservation<T>
        implements EReportingObservation<T>, EReportingValues {

    private static final long serialVersionUID = 2878044983511090422L;

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
    public boolean isSetValidation() {
        return getValidation() != null;
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
    public boolean isSetDataCaptureFlag() {
        return this.dataCaptureFlag != null;
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
    public boolean isSetDataCapture() {
        return this.dataCapture != null;
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
    public boolean isSetTimeCoverageFlag() {
        return this.timeCoverageFlag != null;
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
    public boolean isSetUncertaintyEstimation() {
        return this.uncertaintyEstimation != null;
    }

    @Override
    public String getDiscriminator() {
        return getPrimaryObservation();
    }

}
