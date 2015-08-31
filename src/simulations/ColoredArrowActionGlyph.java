package simulations;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

import burlap.behavior.singleagent.auxiliary.valuefunctionvis.common.ArrowActionGlyph;

public class ColoredArrowActionGlyph extends ArrowActionGlyph {

	private float scale;

	public ColoredArrowActionGlyph(int direction, Color arrowColor) {
		super(direction);
		this.fillColor = arrowColor;
		this.scale = 1f;
	}

	public ColoredArrowActionGlyph(int direction) {
		super(direction);
		this.scale = 1f;
	}

	public ColoredArrowActionGlyph(int direction, double scale) {
		super(direction);
		this.scale = (float) scale;
	}
	
	public ColoredArrowActionGlyph(int direction, Color arrowColor, double scale) {
		super(direction);
		this.fillColor = arrowColor;
		this.scale = (float) scale;
	}

	@Override
	public void paintGlyph(Graphics2D g2, float x, float y, float width,
			float height) {

		int minSize = 30;
		if (width < minSize || height < minSize) {
			return;
		}

		float minDim = Math.min(width, height);

		// Force square for easy drawing
		BufferedImage glyphImage = new BufferedImage((int) minDim,
				(int) minDim, BufferedImage.TYPE_INT_ARGB);
		Graphics2D img = (Graphics2D) glyphImage.getGraphics();

		float cx = width / 2f;
		float cy = height / 2f;
		float sx = cx - (minDim / 2);
		float sy = cy - (minDim / 2);

		float strokeWidth = 0.1f * minDim * this.scale;

		float arrowHeadHeight = 2f * strokeWidth;
		float arrowHeadWidth = 2.5f * strokeWidth;

		float shaftHeight = cy - arrowHeadHeight - sy;

		img.setColor(this.fillColor);

		if (direction < 4) {
			img.fill(new Rectangle2D.Float(cx - (strokeWidth / 2f), sy
					+ arrowHeadHeight, strokeWidth, shaftHeight));

			int[] xTriangle = new int[] { (int) (cx - arrowHeadWidth),
					(int) cx, (int) (cx + arrowHeadWidth) };
			int[] yTriangle = new int[] { (int) arrowHeadHeight, 0,
					(int) arrowHeadHeight };

			Polygon triangle = new Polygon(xTriangle, yTriangle, 3);

			img.fillPolygon(triangle);

			if (this.direction == 0) {
				g2.drawImage(glyphImage, (int) x, (int) y, null);
			} else {
				double locationX = width / 2;
				double locationY = height / 2;
				double rotationRequired = 0.;
				if (this.direction == 1) {
					rotationRequired = Math.PI;
				} else if (this.direction == 2) {
					rotationRequired = Math.PI / 2;
				} else if (this.direction == 3) {
					rotationRequired = 3 * Math.PI / 2;
				}

				AffineTransform tx = AffineTransform.getRotateInstance(
						rotationRequired, locationX, locationY);
				AffineTransformOp op = new AffineTransformOp(tx,
						AffineTransformOp.TYPE_BILINEAR);
				g2.drawImage(op.filter(glyphImage, null), (int) x, (int) y,
						null);

			}
		} else {
			int[] xTriangle = new int[] { (int) cx ,
					(int) cx, (int) (cx - arrowHeadHeight) };
			int[] yTriangle = new int[] { (int) cy+((int)(shaftHeight+arrowHeadWidth)/2), (int) cy+((int)(shaftHeight-arrowHeadWidth)/2),
					(int) cy - (int)shaftHeight/2 };
			
			Polygon triangle = new Polygon(xTriangle, yTriangle, 3);

			img.fillPolygon(triangle);
			
			float radius = 0.65f * shaftHeight;
			img.setStroke(new BasicStroke(strokeWidth));
			img.draw(new Ellipse2D.Float(cx - radius, cy - radius, 2 * radius,
					2 * radius));
			

			double locationX = width / 2;
			double locationY = height / 2;
			double rotationRequired = Math.PI/2;
			AffineTransform tx = AffineTransform.getRotateInstance(
					rotationRequired, locationX, locationY);
			AffineTransformOp op = new AffineTransformOp(tx,
					AffineTransformOp.TYPE_BILINEAR);
			g2.drawImage(op.filter(glyphImage, null), (int) x, (int) y,
					null);
		}
	}
}
