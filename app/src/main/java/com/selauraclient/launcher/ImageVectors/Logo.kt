import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Logo: ImageVector
	get() {
		if (_Logo != null) {
			return _Logo!!
		}
		_Logo = ImageVector.Builder(
            name = "Logo",
            defaultWidth = 500.dp,
            defaultHeight = 500.dp,
            viewportWidth = 500f,
            viewportHeight = 500f
        ).apply {
			path(
    			fill = SolidColor(Color(0xFFFFFFFF)),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(137.048f, 250f)
				lineTo(250f, 137.048f)
				lineTo(362.952f, 250f)
				lineTo(250f, 362.952f)
				lineTo(137.048f, 250f)
				close()
			}
			path(
    			fill = SolidColor(Color(0xFFFFFFFF)),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(0f, 250f)
				lineTo(181.476f, 68.5241f)
				lineTo(219.126f, 106.175f)
				lineTo(37.6506f, 287.651f)
				lineTo(0f, 250f)
				close()
			}
			path(
    			fill = SolidColor(Color(0xFFFFFFFF)),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(280.873f, 393.825f)
				lineTo(371.611f, 303.087f)
				lineTo(462.349f, 212.349f)
				lineTo(500f, 250f)
				lineTo(318.524f, 431.476f)
				lineTo(280.873f, 393.825f)
				close()
			}
			path(
    			fill = SolidColor(Color(0xFFFFFFFF)),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(212.349f, 37.6506f)
				lineTo(250f, 0f)
				lineTo(431.476f, 181.476f)
				lineTo(393.825f, 219.126f)
				lineTo(212.349f, 37.6506f)
				close()
			}
			path(
    			fill = SolidColor(Color(0xFFFFFFFF)),
    			fillAlpha = 1.0f,
    			stroke = null,
    			strokeAlpha = 1.0f,
    			strokeLineWidth = 1.0f,
    			strokeLineCap = StrokeCap.Butt,
    			strokeLineJoin = StrokeJoin.Miter,
    			strokeLineMiter = 1.0f,
    			pathFillType = PathFillType.NonZero
			) {
				moveTo(68.5241f, 318.524f)
				lineTo(106.175f, 280.873f)
				lineTo(287.651f, 462.349f)
				lineTo(250f, 500f)
				lineTo(68.5241f, 318.524f)
				close()
			}
		}.build()
		return _Logo!!
	}

private var _Logo: ImageVector? = null
