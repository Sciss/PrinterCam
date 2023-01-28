/*
 *  PrinterCam.scala
 *  (ReExpo)
 *
 *  Copyright (c) 2023 Hanns Holger Rutz. All rights reserved.
 *
 *  This software is published under the GNU Affero General Public License v3+
 *
 *
 *  For further information, please contact Hanns Holger Rutz at
 *  contact@sciss.de
 */

package de.sciss.printercam

import de.sciss.file.File
import org.rogach.scallop.{ScallopConf, ScallopOption as Opt}

import java.awt.{GraphicsEnvironment, RenderingHints}
import java.awt.event.{InputEvent, KeyEvent}
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.TimerTask
import javax.imageio.ImageIO
import javax.swing.KeyStroke
import scala.swing.{Action, BorderPanel, Component, Dimension, Graphics2D, MainFrame, Menu, MenuBar, MenuItem, Swing}
import scala.util.Try

object PrinterCam {
  case class Config(
                     ip       : String = "",
                     user     : String = "vlc",
                     password : String = "",
                     fps      : Double = 1.0,
                   ) {
  }

  def main(args: Array[String]): Unit = {

    object p extends ScallopConf(args) {

      import org.rogach.scallop.*

      printedName = "PrinterCam"
      private val default = Config()

      val ip: Opt[String] = opt(required = true,
        descr = "Camera's IP address",
      )
      val user: Opt[String] = opt(default = Some(default.user),
        descr = s"IP camera user name (default: ${default.user})",
      )
      val password: Opt[String] = opt(required = true,
        descr = "IP camera user password",
      )
      val fps: Opt[Double] = opt(default = Some(default.fps),
        descr = s"Frames per second (default: ${default.fps})",
      )
//      val shareId: Opt[String] = opt(
//        descr = "Secret RC sharing id if parsing a non-public exposition",
//      )

      verify()
      implicit val config: Config = Config(
        ip        = ip(),
        user      = user(),
        password  = password(),
        fps       = fps(),
      )
    }
    import p.config
    run()
  }

  def grabImage()(implicit config: Config): Option[BufferedImage] = {
    val cmd: Seq[String] = Seq("wget",
      s"http://${config.ip}/cgi-bin/snapshot.cgi",
      "--user", config.user, "--password", config.password, "-O-", "-q"
    )

    import sys.process.*
    val baos = new ByteArrayOutputStream(640 * 1024) // init capacity of 640K should be fine for basically all images
    val res = Process(cmd).#>(baos).!
    if res != 0 then {
      println(s"wget exited with error code $res")
      return None
    }
    val arr = baos.toByteArray
//    val sz = arr.length
//    println(s"array size: $sz")
    val bais = new ByteArrayInputStream(arr)
    Try {
      ImageIO.read(bais)
    } .toOption
  }

  def run()(using config: Config): Unit = {
    import config.*
    import de.sciss.file.*

    import sys.process.*

    var image = new BufferedImage(1920, 1080, BufferedImage.TYPE_INT_RGB)

    Swing.onEDT {
      object View extends Component {
        preferredSize = new Dimension(1920/2, 1080/2)

        private val at = AffineTransform.getScaleInstance(0.5, 0.5)

        override protected def paintComponent(g: Graphics2D): Unit = {
          super.paintComponent(g)
          g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON   )
          g.setRenderingHint(RenderingHints.KEY_RENDERING   , RenderingHints.VALUE_RENDER_QUALITY )
          g.drawImage(image, at, null)
//          g.drawImage(image, 0, 0, null)
        }
      }

      object actionQuit extends Action("Quit") {
        accelerator = Some(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK))

        override def apply(): Unit = sys.exit()
      }

      val grabPeriod = math.max(1L, (1000 / config.fps).toLong)

      val t = new java.util.Timer("grabber")

      val f: MainFrame = new MainFrame {
        title = "PrinterCam"
        contents = new BorderPanel {
//          add(pTop, BorderPanel.Position.North)
          add(View, BorderPanel.Position.Center)
        }
        pack().centerOnScreen()
//        menuBar = new MenuBar {
//          contents += new Menu("File") {
//            contents += new MenuItem(actionQuit)
//          }
//        }

        override def closeOperation(): Unit = {
          t.cancel()
          super.closeOperation()
        }
      }

      f.open()

      t.schedule(new TimerTask {
        override def run(): Unit = {
          grabImage().foreach { imgNew =>
            Swing.onEDT {
              image = imgNew
              View.repaint()
            }
          }
        }
      }, grabPeriod, grabPeriod)

    }
  }
}
