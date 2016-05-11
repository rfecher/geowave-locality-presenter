package mil.nga.giat.geowave.index;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.beust.jcommander.converters.IntegerConverter;

import mil.nga.giat.geowave.core.index.dimension.BasicDimensionDefinition;
import mil.nga.giat.geowave.core.index.sfc.SFCDimensionDefinition;
import mil.nga.giat.geowave.core.index.sfc.SpaceFillingCurve;
import mil.nga.giat.geowave.core.index.sfc.hilbert.HilbertSFC;
import mil.nga.giat.geowave.core.index.sfc.zorder.ZOrderSFC;

public class LocalityPresenter
{
	@Parameter(names = {
		"-s",
		"--pixelsize"
	}, description = "What pixel width and height do you want the output images?")
	private final Integer pixelSize = 256;

	@Parameter(names = {
		"-l",
		"--levels"
	}, variableArity = true, description = "The levels represented", listConverter = IntegerConverter.class)
	private final List<Integer> levels = Arrays.asList(
			1,
			2,
			3,
			4,
			5,
			6,
			7,
			8,
			9,
			10);
	@Parameter(names = {
		"-o",
		"--outputdir"
	}, description = "", converter = FileConverter.class)
	private final File outputDir = null;

	public static void main(
			final String[] args )
			throws IOException {
		final LocalityPresenter presenter = new LocalityPresenter();
		final JCommander commander = new JCommander(
				presenter);
		commander.parse(
				args);
		for (final int l : presenter.levels) {
			int actualPixelSize = (int) Math.max(presenter.pixelSize, Math.pow(2, l));
			final BasicDimensionDefinition xPixel = new BasicDimensionDefinition(
					0,
					actualPixelSize - 1);
			final BasicDimensionDefinition yPixel = new BasicDimensionDefinition(
					0,
					actualPixelSize - 1);
			final SFCDimensionDefinition[] defs = new SFCDimensionDefinition[] {
				new SFCDimensionDefinition(
						xPixel,
						l),
				new SFCDimensionDefinition(
						yPixel,
						l)
			};
			final Pair<String, SpaceFillingCurve>[] sfcs = new Pair[] {
				new ImmutablePair<String, SpaceFillingCurve>(
						"z",
						new ZOrderSFC(
								defs)),
				new ImmutablePair<String, SpaceFillingCurve>(
						"h",
						new HilbertSFC(
								defs))
			};
			final byte[] min = new byte[(int) Math.ceil(
					Math.pow(
							2,
							l) / 4)];
			final byte[] max = new byte[(int) Math.ceil(
					Math.pow(
							2,
							l) / 4)];
			for (int i = 0; i < min.length; i++) {
				min[i] = 0;
				// -1 is 0xff
				max[i] = -1;
			}
			for (final Pair<String, SpaceFillingCurve> sfc : sfcs) {
				final BufferedImage img = new BufferedImage(
						actualPixelSize,
						actualPixelSize,
						BufferedImage.TYPE_BYTE_GRAY);
				final Graphics2D g = img.createGraphics();
				for (int x = 0; x < actualPixelSize; x++) {
					for (int y = 0; y < actualPixelSize; y++) {
						final byte[] sfcId = sfc.getRight().getId(
								new double[] {
									x,
									(actualPixelSize - 1) - y // invert y so
																	// the
																	// bottom is
																	// 0
								});
						final float percentile = getPercentileOfRange(
								sfcId,
								l);
						g.setColor(
								new Color(
										percentile,
										percentile,
										percentile));
						g.fillRect(
								x,
								y,
								1,
								1);
					}
				}
				final String outFilename = sfc.getLeft() + "-" + l + ".png";

				File outputImage;
				if (presenter.outputDir != null) {
					presenter.outputDir.mkdirs();
					outputImage = new File(
							presenter.outputDir,
							outFilename);
				}
				else {
					outputImage = new File(
							outFilename);
				}
				outputImage.delete();
				outputImage.createNewFile();
				ImageIO.write(
						img,
						"png",
						outputImage);
			}
		}

	}

	// private static byte[] extractBytes(
	// final byte[] original,
	// final int numBytes ) {
	// return extractBytes(
	// original,
	// numBytes,
	// false);
	// }
	//
	// private static byte[] extractBytes(
	// final byte[] original,
	// final int numBytes,
	// final boolean infiniteEndKey ) {
	// final byte[] bytes = new byte[numBytes + 2];
	// bytes[0] = 1;
	// bytes[1] = 0;
	// for (int i = 0; i < numBytes; i++) {
	// if (i >= original.length) {
	// if (infiniteEndKey) {
	// // -1 is 0xff
	// bytes[i + 2] = -1;
	// }
	// else {
	// bytes[i + 2] = 0;
	// }
	// }
	// else {
	// bytes[i + 2] = original[i];
	// }
	// }
	// return bytes;
	// }

	private static long getNumeric(
			final byte[] bytes ) {
		long sum = 0;
		for (int i = 0; i < bytes.length; i++) {
			sum += (bytes[i] & 0xFF) * Math.pow(
					256,
					bytes.length - i - 1);
		}
		return sum;
	}

	private static float getPercentileOfRange(
			final byte[] position,
			final int numBits ) {
		// final int maxDepth = Math.min(
		// Math.max(
		// end.length,
		// start.length),
		// position.length);
		// final BigInteger startBI = new BigInteger(
		// extractBytes(
		// start,
		// maxDepth));
		// final BigInteger endBI = new BigInteger(
		// extractBytes(
		// end,
		// maxDepth));
		// final BigInteger positionBI = new BigInteger(
		// extractBytes(
		// position,
		// maxDepth));
		// return (float) (positionBI.subtract(
		// startBI).doubleValue()
		// / endBI.subtract(
		// startBI).doubleValue());
		final long startNumeric = 0;

		final long endNumeric = (long) Math.pow(
				2,
				numBits * 2) - 1;
		final long positionNumeric = getNumeric(
				position);
		return (float) ((double) (positionNumeric - startNumeric) / (double) (endNumeric - startNumeric));
	}
}
