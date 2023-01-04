/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.healthconnect.internal.datatypes;

import android.annotation.NonNull;
import android.healthconnect.datatypes.Identifier;
import android.healthconnect.datatypes.RecordTypeIdentifier;
import android.healthconnect.datatypes.StepsCadenceRecord;
import android.os.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @see StepsCadenceRecord
 * @hide
 */
@Identifier(recordIdentifier = RecordTypeIdentifier.RECORD_TYPE_STEPS_CADENCE)
public class StepsCadenceRecordInternal
        extends SeriesRecordInternal<
                StepsCadenceRecord, StepsCadenceRecord.StepsCadenceRecordSample> {
    private List<StepsCadenceRecordSample> mStepsCadenceRecordSamples = new ArrayList<>();

    @Override
    void populateIntervalRecordFrom(@NonNull StepsCadenceRecord stepsCadenceRecord) {
        mStepsCadenceRecordSamples = new ArrayList<>(stepsCadenceRecord.getSamples().size());
        for (StepsCadenceRecord.StepsCadenceRecordSample stepsCadenceRecordSample :
                stepsCadenceRecord.getSamples()) {
            mStepsCadenceRecordSamples.add(
                    new StepsCadenceRecordSample(
                            stepsCadenceRecordSample.getRate(),
                            stepsCadenceRecordSample.getTime().toEpochMilli()));
        }
    }

    @Override
    @NonNull
    public List<StepsCadenceRecordSample> getSamples() {
        return mStepsCadenceRecordSamples;
    }

    @NonNull
    @Override
    public StepsCadenceRecordInternal setSamples(List<? extends Sample> samples) {
        Objects.requireNonNull(samples);
        this.mStepsCadenceRecordSamples = (List<StepsCadenceRecordSample>) samples;
        return this;
    }

    @Override
    @NonNull
    public StepsCadenceRecord toExternalRecord() {
        return new StepsCadenceRecord.Builder(
                        buildMetaData(), getStartTime(), getEndTime(), getExternalSamples())
                .setStartZoneOffset(getStartZoneOffset())
                .setEndZoneOffset(getEndZoneOffset())
                .build();
    }

    @Override
    void populateIntervalRecordFrom(@NonNull Parcel parcel) {
        int size = parcel.readInt();
        mStepsCadenceRecordSamples = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            mStepsCadenceRecordSamples.add(
                    new StepsCadenceRecordSample(parcel.readDouble(), parcel.readLong()));
        }
    }

    private List<StepsCadenceRecord.StepsCadenceRecordSample> getExternalSamples() {
        List<StepsCadenceRecord.StepsCadenceRecordSample> stepsCadenceRecords =
                new ArrayList<>(mStepsCadenceRecordSamples.size());
        for (StepsCadenceRecordSample stepsCadenceRecordSample : mStepsCadenceRecordSamples) {
            stepsCadenceRecords.add(
                    new StepsCadenceRecord.StepsCadenceRecordSample(
                            stepsCadenceRecordSample.getRate(),
                            Instant.ofEpochMilli(stepsCadenceRecordSample.getEpochMillis())));
        }
        return stepsCadenceRecords;
    }

    @Override
    void populateIntervalRecordTo(@NonNull Parcel parcel) {
        parcel.writeInt(mStepsCadenceRecordSamples.size());
        for (StepsCadenceRecordSample stepsCadenceRecordSample : mStepsCadenceRecordSamples) {
            parcel.writeDouble(stepsCadenceRecordSample.getRate());
            parcel.writeLong(stepsCadenceRecordSample.getEpochMillis());
        }
    }

    /**
     * @see android.healthconnect.datatypes.StepsCadenceRecord.StepsCadenceRecordSample
     */
    public static final class StepsCadenceRecordSample implements Sample {
        private final double mRate;
        private final long mEpochMillis;

        public StepsCadenceRecordSample(double rate, long epochMillis) {
            mRate = rate;
            mEpochMillis = epochMillis;
        }

        public double getRate() {
            return mRate;
        }

        public long getEpochMillis() {
            return mEpochMillis;
        }
    }
}
