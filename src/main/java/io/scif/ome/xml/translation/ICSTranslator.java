/*
 * #%L
 * SCIFIO Bio-Formats compatibility format.
 * %%
 * Copyright (C) 2013 Open Microscopy Environment:
 *   - Board of Regents of the University of Wisconsin-Madison
 *   - Glencoe Software, Inc.
 *   - University of Dundee
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package io.scif.ome.xml.translation;

import io.scif.FormatException;
import io.scif.MetadataLevel;
import io.scif.MetadataOptions;
import io.scif.formats.ICSFormat;
import io.scif.ome.xml.meta.OMEMetadata;
import io.scif.ome.xml.meta.OMEXMLMetadata;
import io.scif.ome.xml.services.OMEXMLMetadataService;
import io.scif.util.FormatTools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import loci.formats.meta.FilterMetadata;
import loci.formats.meta.MetadataRetrieve;
import net.imglib2.meta.Axes;
import ome.xml.model.primitives.PositiveFloat;
import ome.xml.model.primitives.PositiveInteger;
import ome.xml.model.primitives.Timestamp;

import org.scijava.Priority;
import org.scijava.plugin.Attr;
import org.scijava.plugin.Plugin;

/**
 * Container class for to and from ICS/OME formats
 * 
 * @author Mark Hiner hinerm at gmail.com
 */
public class ICSTranslator {

	/**
	 * Translator class from {@link io.scif.formats.ICSFormat.Metadata} to
	 * {@link ome.xml.meta.OMEMetadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = FromOMETranslator.class, priority = Priority.HIGH_PRIORITY,
		attrs = { @Attr(name = OMEICSTranslator.SOURCE, value = OMEMetadata.CNAME),
			@Attr(name = OMEICSTranslator.DEST, value = ICSFormat.Metadata.CNAME) })
	public static class OMEICSTranslator extends
		FromOMETranslator<ICSFormat.Metadata>
	{

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final OMEMetadata source,
			final ICSFormat.Metadata dest)
		{
			super.typedTranslate(source, dest);

			final MetadataRetrieve retrieve = source.getRoot();

			final Timestamp ts = retrieve.getImageAcquisitionDate(0);

			if (ts != null) dest.putDate(ts.getValue());

			final MetadataOptions options = source.getMetadataOptions();

			if (options != null &&
				options.getMetadataLevel() != MetadataLevel.MINIMUM)
			{
				dest.putDescription(retrieve.getImageDescription(0));

				if (retrieve.getInstrumentCount() > 0) {
					dest.putMicroscopeModel(retrieve.getMicroscopeModel(0));
					dest.putMicroscopeManufacturer(retrieve.getMicroscopeManufacturer(0));
					final Hashtable<Integer, Integer> laserWaves =
						new Hashtable<Integer, Integer>();

					for (int i = 0; i < retrieve.getLightSourceCount(0); i++) {
						laserWaves.put(i, retrieve.getLaserWavelength(0, i).getValue());
					}

					dest.putWavelengths(laserWaves);
					dest.putLaserManufacturer(retrieve.getLaserManufacturer(0, 0));
					dest.putLaserModel(retrieve.getLaserModel(0, 0));
					dest.putLaserPower(retrieve.getLaserPower(0, 0));
					dest.putLaserRepetitionRate(retrieve.getLaserRepetitionRate(0, 0));

					dest.putFilterSetModel(retrieve.getFilterSetModel(0, 0));
					dest.putDichroicModel(retrieve.getDichroicModel(0, 0));
					dest.putExcitationModel(retrieve.getFilterModel(0, 0));
					dest.putEmissionModel(retrieve.getFilterModel(0, 1));

					dest.putObjectiveModel(retrieve.getObjectiveModel(0, 0));
					dest.putImmersion(retrieve.getObjectiveImmersion(0, 0).getValue());
					dest.putLensNA(retrieve.getObjectiveLensNA(0, 0));
					dest.putWorkingDistance(retrieve.getObjectiveWorkingDistance(0, 0));
					dest.putMagnification(retrieve.getObjectiveCalibratedMagnification(0,
						0));

					dest.putDetectorManufacturer(retrieve.getDetectorManufacturer(0, 0));
					dest.putDetectorModel(retrieve.getDetectorModel(0, 0));
				}

				if (retrieve.getExperimentCount() > 0) {
					dest.putExperimentType(retrieve.getExperimentType(0).getValue());
					dest.putAuthorLastName(retrieve.getExperimenterLastName(0));
				}

				final Double[] pixelSizes = new Double[5];
				final String[] units = new String[5];

				final String order = retrieve.getPixelsDimensionOrder(0).getValue();
				final PositiveFloat sizex = retrieve.getPixelsPhysicalSizeX(0), sizey =
					retrieve.getPixelsPhysicalSizeY(0), sizez =
					retrieve.getPixelsPhysicalSizeZ(0);
				final Double sizet = retrieve.getPixelsTimeIncrement(0);

				for (int i = 0; i < order.length(); i++) {
					switch (order.toUpperCase().charAt(i)) {
						case 'X':
							pixelSizes[i] = sizex == null ? 1.0 : sizex.getValue();
							units[i] = "um";
							break;
						case 'Y':
							pixelSizes[i] = sizey == null ? 1.0 : sizey.getValue();
							units[i] = "um";
							break;
						case 'Z':
							pixelSizes[i] = sizez == null ? 1.0 : sizez.getValue();
							units[i] = "um";
							break;
						case 'T':
							pixelSizes[i] = sizet == null ? 1.0 : sizet;
							units[i] = "s";
							break;
						case 'C':
							pixelSizes[i] = 1.0;
							units[i] = "um";
							break;
						default:
							pixelSizes[i] = 1.0;
							units[i] = "um";
					}
				}

				dest.putPixelSizes(pixelSizes);
				dest.putUnits(units);

				if (retrieve.getPlaneCount(0) > 0) {
					final Double[] timestamps =
						new Double[source.getAxisLength(0, Axes.TIME)];

					for (int t = 0; t < timestamps.length; t++) {
						for (int z = 0; z < source.getAxisLength(0, Axes.Z); z++) {
							for (int c = 0; c < source.getEffectiveSizeC(0); c++) {
								final int index =
									FormatTools.getIndex(FormatTools
										.findDimensionOrder(source, 0), source.getAxisLength(0,
										Axes.Z), source.getEffectiveSizeC(0), source.getAxisLength(
										0, Axes.TIME), source.getPlaneCount(0), z, c, t);
								timestamps[t] = retrieve.getPlaneDeltaT(0, index);
							}
						}
					}

					dest.putTimestamps(timestamps);
					dest.putExposureTime(retrieve.getPlaneExposureTime(0, 0));
				}

				final Hashtable<Integer, String> channelNames =
					new Hashtable<Integer, String>();
				final Hashtable<Integer, Double> pinholes =
					new Hashtable<Integer, Double>();
				final Hashtable<Integer, Double> gains =
					new Hashtable<Integer, Double>();
				final List<Integer> emWaves = new ArrayList<Integer>();
				final List<Integer> exWaves = new ArrayList<Integer>();

				for (int i = 0; i < source.getEffectiveSizeC(0); i++) {
					final String cName = retrieve.getChannelName(0, i);
					if (cName != null) channelNames.put(i, cName);

					final Double pinSize = retrieve.getChannelPinholeSize(0, i);
					if (pinSize != null) pinholes.put(i, pinSize);

					final PositiveInteger emWave =
						retrieve.getChannelEmissionWavelength(0, i);
					if (emWave != null) emWaves.add(emWave.getValue());

					final PositiveInteger exWave =
						retrieve.getChannelExcitationWavelength(0, i);
					if (exWave != null) exWaves.add(exWave.getValue());

					if (retrieve.getInstrumentCount() > 0 &&
						retrieve.getDetectorCount(0) > 0)
					{
						final Double chGain = retrieve.getDetectorSettingsGain(0, i);
						if (chGain != null) gains.put(i, chGain);
					}
				}

				dest.putChannelNames(channelNames);
				dest.putPinholes(pinholes);
				dest.putEXWaves(exWaves.toArray(new Integer[exWaves.size()]));
				dest.putEMWaves(emWaves.toArray(new Integer[emWaves.size()]));
				dest.putGains(gains);
			}
		}
	}

	/**
	 * Translator class from {@link ICSMetadata} to {@link OMEMetadata}.
	 * <p>
	 * NB: Plugin priority is set to high to be selected over the base
	 * {@link io.scif.Metadata} translator.
	 * </p>
	 * 
	 * @author Mark Hiner
	 */
	@Plugin(type = ToOMETranslator.class, priority = Priority.HIGH_PRIORITY,
		attrs = {
			@Attr(name = ICSOMETranslator.SOURCE, value = ICSFormat.Metadata.CNAME),
			@Attr(name = ICSOMETranslator.DEST, value = OMEMetadata.CNAME) })
	public static class ICSOMETranslator extends
		ToOMETranslator<ICSFormat.Metadata>
	{

		// -- Translator API --

		/*
		 * @see OMETranslator#typedTranslate(io.scif.Metadata, io.scif.Metadata)
		 */
		@Override
		protected void typedTranslate(final ICSFormat.Metadata source,
			final OMEMetadata dest)
		{
			final OMEXMLMetadata store = dest.getRoot();
			boolean lifetime = false;
			final int imageIndex = 0; // TODO correct index?
			Double[] pixelSizes = null, timestamps = null, stagePos = null;
			Double laserPower = null, laserRepetitionRate = null, lensNA = null, workingDistance =
				null, magnification = null, exposureTime = null;
			Integer[] emWaves = null, exWaves = null;
			double[] sizes = null;
			String[] units = null, axes = null;
			final String imageName = source.getSource().getFileName();
			String date = null, description = null, microscopeModel = null, microscopeManufacturer =
				null, experimentType = null, laserManufacturer = null, laserModel =
				null, filterSetModel = null, dichroicModel = null, excitationModel =
				null, emissionModel = null, objectiveModel = null, immersion = null, detectorManufacturer =
				null, detectorModel = null, lastName = null;

			Hashtable<Integer, Double> gains = new Hashtable<Integer, Double>();
			Hashtable<Integer, String> channelNames =
				new Hashtable<Integer, String>();
			Hashtable<Integer, Double> pinholes = new Hashtable<Integer, Double>();
			Hashtable<Integer, Integer> wavelengths =
				new Hashtable<Integer, Integer>();

			final FilterMetadata filter =
				new FilterMetadata(store, source.isFiltered());
			filter.createRoot();

			// FIXME: no more datasetmetadata

			getContext().getService(OMEXMLMetadataService.class).populatePixels(
				filter, source, true);

			store.setImageName(imageName, 0);

			// populate date data

			date = source.getDate();

			if (date != null) store.setImageAcquisitionDate(new Timestamp(date), 0);

			// > Minimum Metadata population
			if (source.getMetadataOptions().getMetadataLevel() != MetadataLevel.MINIMUM)
			{

				description = source.getDescription();
				store.setImageDescription(description, 0);

				// link Instrument and Image
				final String instrumentID =
					getContext().getService(OMEXMLMetadataService.class).createLSID(
						"Instrument", 0);
				store.setInstrumentID(instrumentID, 0);

				microscopeModel = source.getMicroscopeModel();
				store.setMicroscopeModel(microscopeModel, 0);

				microscopeManufacturer = source.getMicroscopeManufacturer();
				store.setMicroscopeManufacturer(microscopeManufacturer, 0);

				store.setImageInstrumentRef(instrumentID, 0);

				store.setExperimentID(getContext().getService(
					OMEXMLMetadataService.class).createLSID("Experiment", 0), 0);

				lifetime = source.getLifetime();

				experimentType = source.getExperimentType();

				try {
					store.setExperimentType(getContext().getService(
						OMEXMLMetadataService.class).getExperimentType(experimentType), 0);
				}
				catch (final FormatException e) {
					LOGGER.debug("Could not set experiment type", e);
				}

				// populate Dimensions data

				pixelSizes = source.getPixelSizes();

				units = source.getUnits();

				axes = source.getAxes();

				sizes = source.getAxesSizes();

				if (pixelSizes != null) {
					if (units != null && units.length == pixelSizes.length - 1) {
						// correct for missing units
						// sometimes, the units for the C axis are missing entirely
						final ArrayList<String> realUnits = new ArrayList<String>();
						int unitIndex = 0;
						for (int i = 0; i < axes.length; i++) {
							if (axes[i].toLowerCase().equals("ch")) {
								realUnits.add("nm");
							}
							else {
								realUnits.add(units[unitIndex++]);
							}
						}
						units = realUnits.toArray(new String[realUnits.size()]);
					}

					for (int i = 0; i < pixelSizes.length; i++) {
						final Double pixelSize = pixelSizes[i];

						if (pixelSize == null) continue;

						final String axis = axes != null && axes.length > i ? axes[i] : "";
						final String unit =
							units != null && units.length > i ? units[i] : "";
						if (axis.equals("x")) {
							if (pixelSize > 0 &&
								checkUnit(unit, "um", "microns", "micrometers"))
							{
								store.setPixelsPhysicalSizeX(new PositiveFloat(pixelSize), 0);
							}
							else {
								LOGGER.warn(
									"Expected positive value for PhysicalSizeX; got {}",
									pixelSize);
							}
						}
						else if (axis.equals("y")) {
							if (pixelSize > 0 &&
								checkUnit(unit, "um", "microns", "micrometers"))
							{
								store.setPixelsPhysicalSizeY(new PositiveFloat(pixelSize), 0);
							}
							else {
								LOGGER.warn(
									"Expected positive value for PhysicalSizeY; got {}",
									pixelSize);
							}
						}
						else if (axis.equals("z")) {
							if (pixelSize > 0 &&
								checkUnit(unit, "um", "microns", "micrometers"))
							{
								store.setPixelsPhysicalSizeZ(new PositiveFloat(pixelSize), 0);
							}
							else {
								LOGGER.warn(
									"Expected positive value for PhysicalSizeZ; got {}",
									pixelSize);
							}
						}
						else if (axis.equals("t")) {
							if (checkUnit(unit, "ms")) {
								store.setPixelsTimeIncrement(1000 * pixelSize, 0);
							}
							else if (checkUnit(unit, "seconds") || checkUnit(unit, "s")) {
								store.setPixelsTimeIncrement(pixelSize, 0);
							}
						}
					}
				}
				else if (sizes != null) {
					if (sizes.length > 0 && sizes[0] > 0) {
						store.setPixelsPhysicalSizeX(new PositiveFloat(sizes[0]), 0);
					}
					else {
						LOGGER.warn("Expected positive value for PhysicalSizeX; got {}",
							sizes[0]);
					}
					if (sizes.length > 1) {
						sizes[1] /= source.getAxisLength(imageIndex, Axes.Y);
						if (sizes[1] > 0) {
							store.setPixelsPhysicalSizeY(new PositiveFloat(sizes[1]), 0);
						}
						else {
							LOGGER.warn("Expected positive value for PhysicalSizeY; got {}",
								sizes[1]);
						}
					}
				}
			}

			// populate Plane data

			timestamps = source.getTimestamps();

			if (timestamps != null) {
				for (int t = 0; t < timestamps.length; t++) {
					if (t >= source.getAxisLength(imageIndex, Axes.TIME)) break; // ignore
																																				// superfluous
																																				// timestamps
					if (timestamps[t] == null) continue; // ignore missing timestamp
					final double deltaT = timestamps[t];
					if (Double.isNaN(deltaT)) continue; // ignore invalid timestamp
					// assign timestamp to all relevant planes
					for (int z = 0; z < source.getAxisLength(imageIndex, Axes.Z); z++) {
						for (int c = 0; c < source.getEffectiveSizeC(imageIndex); c++) {
							final int index =
								FormatTools.getIndex(FormatTools.findDimensionOrder(source,
									imageIndex), imageIndex, source.getAxisLength(imageIndex,
									Axes.Z), source.getEffectiveSizeC(imageIndex), source
									.getAxisLength(imageIndex, Axes.TIME), z, c, t);
							store.setPlaneDeltaT(deltaT, 0, index);
						}
					}
				}
			}

			// populate LogicalChannel data

			channelNames = source.getChannelNames();
			source.addStepChannel(channelNames);
			source.addCubeChannel(channelNames);

			pinholes = source.getPinholes();

			emWaves = source.getEMWaves();

			if (emWaves == null) {
				emWaves = source.getEMSingleton();
			}

			exWaves = source.getEXWaves();

			if (exWaves == null) {
				exWaves = source.getEXSingleton();
			}

			for (int i = 0; i < source.getEffectiveSizeC(imageIndex); i++) {
				if (channelNames.containsKey(i)) {
					store.setChannelName(channelNames.get(i), 0, i);
				}
				if (pinholes.containsKey(i)) {
					store.setChannelPinholeSize(pinholes.get(i), 0, i);
				}
				if (emWaves != null && i < emWaves.length) {
					if (emWaves[i].intValue() > 0) {
						store.setChannelEmissionWavelength(new PositiveInteger(emWaves[i]),
							0, i);
					}
					else {
						LOGGER.warn(
							"Expected positive value for EmissionWavelength; got {}",
							emWaves[i]);
					}
				}
				if (exWaves != null && i < exWaves.length) {
					if (exWaves[i].intValue() > 0) {
						store.setChannelExcitationWavelength(
							new PositiveInteger(exWaves[i]), 0, i);
					}
					else {
						LOGGER.warn(
							"Expected positive value for ExcitationWavelength; got {}",
							exWaves[i]);
					}
				}
			}

			// populate Laser data

			wavelengths = source.getWavelengths();
			source.addLaserWavelength(wavelengths);

			laserManufacturer = source.getLaserManufacturer();

			laserModel = source.getLaserModel();

			laserPower = source.getLaserPower();

			laserRepetitionRate = source.getLaserRepetitionRate();

			final Integer[] lasers = wavelengths.keySet().toArray(new Integer[0]);
			Arrays.sort(lasers);
			for (int i = 0; i < lasers.length; i++) {
				store.setLaserID(getContext().getService(OMEXMLMetadataService.class)
					.createLSID("LightSource", 0, i), 0, i);
				if (wavelengths.get(lasers[i]) > 0) {
					store.setLaserWavelength(new PositiveInteger(wavelengths
						.get(lasers[i])), 0, i);
				}
				else {
					LOGGER.warn("Expected positive value for wavelength; got {}",
						wavelengths.get(lasers[i]));
				}

				try {
					store.setLaserType(getContext().getService(
						OMEXMLMetadataService.class).getLaserType("Other"), 0, i);
				}
				catch (final FormatException e) {
					LOGGER.warn("Failed to set laser type", e);
				}

				try {
					store.setLaserLaserMedium(getContext().getService(
						OMEXMLMetadataService.class).getLaserMedium("Other"), 0, i);
				}
				catch (final FormatException e) {
					LOGGER.warn("Failed to set laser medium", e);
				}

				store.setLaserManufacturer(laserManufacturer, 0, i);
				store.setLaserModel(laserModel, 0, i);
				store.setLaserPower(laserPower, 0, i);
				store.setLaserRepetitionRate(laserRepetitionRate, 0, i);
			}

			if (lasers.length == 0 && laserManufacturer != null) {
				store.setLaserID(getContext().getService(OMEXMLMetadataService.class)
					.createLSID("LightSource", 0, 0), 0, 0);

				try {
					store.setLaserType(getContext().getService(
						OMEXMLMetadataService.class).getLaserType("Other"), 0, 0);
				}
				catch (final FormatException e) {
					LOGGER.warn("Failed to set laser type", e);
				}
				try {
					store.setLaserLaserMedium(getContext().getService(
						OMEXMLMetadataService.class).getLaserMedium("Other"), 0, 0);
				}
				catch (final FormatException e) {
					LOGGER.warn("Failed to set laser medium", e);
				}
				store.setLaserManufacturer(laserManufacturer, 0, 0);
				store.setLaserModel(laserModel, 0, 0);
				store.setLaserPower(laserPower, 0, 0);
				store.setLaserRepetitionRate(laserRepetitionRate, 0, 0);
			}

			// populate FilterSet data

			filterSetModel = source.getFilterSetModel();

			dichroicModel = source.getDichroicModel();

			excitationModel = source.getExcitationModel();

			emissionModel = source.getEmissionModel();

			if (filterSetModel != null) {
				store.setFilterSetID(getContext().getService(
					OMEXMLMetadataService.class).createLSID("FilterSet", 0, 0), 0, 0);
				store.setFilterSetModel(filterSetModel, 0, 0);

				final String dichroicID =
					getContext().getService(OMEXMLMetadataService.class).createLSID(
						"Dichroic", 0, 0);
				final String emFilterID =
					getContext().getService(OMEXMLMetadataService.class).createLSID(
						"Filter", 0, 0);
				final String exFilterID =
					getContext().getService(OMEXMLMetadataService.class).createLSID(
						"Filter", 0, 1);

				store.setDichroicID(dichroicID, 0, 0);
				store.setDichroicModel(dichroicModel, 0, 0);
				store.setFilterSetDichroicRef(dichroicID, 0, 0);

				store.setFilterID(emFilterID, 0, 0);
				store.setFilterModel(emissionModel, 0, 0);
				store.setFilterSetEmissionFilterRef(emFilterID, 0, 0, 0);

				store.setFilterID(exFilterID, 0, 1);
				store.setFilterModel(excitationModel, 0, 1);
				store.setFilterSetExcitationFilterRef(exFilterID, 0, 0, 0);
			}

			// populate Objective data

			objectiveModel = source.getObjectiveModel();

			immersion = source.getImmersion();

			lensNA = source.getLensNA();

			workingDistance = source.getWorkingDistance();

			magnification = source.getMagnification();

			if (objectiveModel != null) store.setObjectiveModel(objectiveModel, 0, 0);
			if (immersion == null) immersion = "Other";
			try {
				store.setObjectiveImmersion(getContext().getService(
					OMEXMLMetadataService.class).getImmersion(immersion), 0, 0);
			}
			catch (final FormatException e) {
				LOGGER.warn("failed to set objective immersion", e);
			}
			if (lensNA != null) store.setObjectiveLensNA(lensNA, 0, 0);
			if (workingDistance != null) {
				store.setObjectiveWorkingDistance(workingDistance, 0, 0);
			}
			if (magnification != null) {
				store.setObjectiveCalibratedMagnification(magnification, 0, 0);
			}
			try {
				store.setObjectiveCorrection(getContext().getService(
					OMEXMLMetadataService.class).getCorrection("Other"), 0, 0);
			}
			catch (final FormatException e) {
				LOGGER.warn("Failed to store objective correction", e);
			}

			// link Objective to Image
			final String objectiveID =
				getContext().getService(OMEXMLMetadataService.class).createLSID(
					"Objective", 0, 0);
			store.setObjectiveID(objectiveID, 0, 0);
			store.setObjectiveSettingsID(objectiveID, 0);

			// populate Detector data

			detectorManufacturer = source.getDetectorManufacturer();

			detectorModel = source.getDetectorModel();

			final String detectorID =
				getContext().getService(OMEXMLMetadataService.class).createLSID(
					"Detector", 0, 0);
			store.setDetectorID(detectorID, 0, 0);
			store.setDetectorManufacturer(detectorManufacturer, 0, 0);
			store.setDetectorModel(detectorModel, 0, 0);
			try {
				store.setDetectorType(getContext().getService(
					OMEXMLMetadataService.class).getDetectorType("Other"), 0, 0);
			}
			catch (final FormatException e) {
				LOGGER.warn("Failed to store detector type", e);
			}

			gains = source.getGains();

			for (final Integer key : gains.keySet()) {
				final int index = key.intValue();
				if (index < source.getEffectiveSizeC(imageIndex)) {
					store.setDetectorSettingsGain(gains.get(key), 0, index);
					store.setDetectorSettingsID(detectorID, 0, index);
				}
			}

			// populate Experimenter data

			lastName = source.getAuthorLastName();

			if (lastName != null) {
				final String experimenterID =
					getContext().getService(OMEXMLMetadataService.class).createLSID(
						"Experimenter", 0);
				store.setExperimenterID(experimenterID, 0);
				store.setExperimenterLastName(lastName, 0);
			}

			// populate StagePosition data

			stagePos = source.getStagePositions();

			if (stagePos == null) {
				stagePos = new Double[3];
			}

			stagePos[0] = source.getStageX();

			stagePos[1] = source.getStageY();

			stagePos[2] = source.getStageZ();

			// TODO set global meta - x, y, z positions?

			exposureTime = source.getExposureTime();

			if (exposureTime != null) {
				for (int i = 0; i < source.getImageCount(); i++) {
					store.setPlaneExposureTime(exposureTime, 0, i);
				}
			}

			dest.setRoot(store);
		}

		// -- Helper methods --

		/*
		 * String tokenizer for parsing metadata. Splits on any white-space
		 * characters. Tabs and spaces are often used interchangeably in real-life ICS
		 * files.
		 *
		 * Also splits on 0x04 character which appears in "paul/csarseven.ics" and
		 * "paul/gci/time resolved_1.ics".
		 *
		 * Also respects double quote marks, so that
		 *   Modules("Confocal C1 Grabber").BarrierFilter(2)
		 * is just one token.
		 *
		 * If not for the last requirement, the one line
		 *   String[] tokens = line.split("[\\s\\x04]+");
		 * would work.
		 */
		private String[] tokenize(final String line) {
			final List<String> tokens = new ArrayList<String>();
			boolean inWhiteSpace = true;
			boolean withinQuotes = false;
			StringBuffer token = null;
			for (int i = 0; i < line.length(); ++i) {
				final char c = line.charAt(i);
				if (Character.isWhitespace(c) || c == 0x04) {
					if (withinQuotes) {
						// retain white space within quotes
						token.append(c);
					}
					else if (!inWhiteSpace) {
						// put out pending token string
						inWhiteSpace = true;
						if (token.length() > 0) {
							tokens.add(token.toString());
							token = null;
						}
					}
				}
				else {
					if ('"' == c) {
						// toggle quotes
						withinQuotes = !withinQuotes;
					}
					if (inWhiteSpace) {
						inWhiteSpace = false;
						// start a new token string
						token = new StringBuffer();
					}
					// build token string
					token.append(c);
				}
			}
			// put out any pending token strings
			if (null != token && token.length() > 0) {
				tokens.add(token.toString());
			}
			return tokens.toArray(new String[0]);
		}

		/** Verifies that a unit matches the expected value. */
		private boolean checkUnit(final String actual, final String... expected) {
			if (actual == null || actual.equals("")) return true; // undefined is OK
			for (final String exp : expected) {
				if (actual.equals(exp)) return true; // unit matches expected value
			}
			LOGGER.debug("Unexpected unit '{}'; expected '{}'", actual, expected);
			return false;
		}
	}
}
