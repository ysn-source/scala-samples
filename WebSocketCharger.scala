
import scala.concurrent.duration._

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.jdbc.Predef._

class WebSocketCharger extends Simulation {

  private val httpProtocol = http
    .baseUrl("http://localhost:3000")
    .inferHtmlResources(AllowList(), DenyList())
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9")
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("fr-FR,fr;q=0.9,en-US;q=0.8,en;q=0.7")
    .upgradeInsecureRequestsHeader("1")
    .userAgentHeader("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36")
  
  private val headers_0 = Map(
  		"Cache-Control" -> "no-cache",
  		"Pragma" -> "no-cache",
  		"Sec-Fetch-Dest" -> "document",
  		"Sec-Fetch-Mode" -> "navigate",
  		"Sec-Fetch-Site" -> "none",
  		"Sec-Fetch-User" -> "?1",
  		"sec-ch-ua" -> """Chromium";v="95", ";Not A Brand";v="99""",
  		"sec-ch-ua-mobile" -> "?0",
  		"sec-ch-ua-platform" -> "Windows"
  )
  
  private val headers_1 = Map(
  		"Cache-Control" -> "no-cache",
  		"Origin" -> "http://localhost:3000",
  		"Pragma" -> "no-cache",
  		"Sec-Fetch-Dest" -> "document",
  		"Sec-Fetch-Mode" -> "navigate",
  		"Sec-Fetch-Site" -> "same-origin",
  		"Sec-Fetch-User" -> "?1",
  		"sec-ch-ua" -> """Chromium";v="95", ";Not A Brand";v="99""",
  		"sec-ch-ua-mobile" -> "?0",
  		"sec-ch-ua-platform" -> "Windows"
  )


  private val scn = scenario("Web Socket Charger")
    .exec(http("home").get("/").headers(headers_0))
    //.exec(http("creation").post("/create/1639634953885").headers(headers_1).formParam("roomname", "BMvzSXvNnU"))
	.exec(session => session.set("nickname", session.userId))
	.exec(session => session.set("channel", "jfRcDqokRr"))
    .exec(http("join").post("/join/#{channel}").headers(headers_1).formParam("nick", "#{nickname}"))
    .exec(ws("open","chatroom").connect("ws://localhost:3000/ws/#{channel}").subprotocol("DEBUG")
		.onConnected(
    		exec(exec(ws("connect","chatroom").sendText("""{"type": "CONNECT","nick": "#{nickname}","room": "#{channel}"}""")))
			.during(20) {
				group("send/receive msg") {
					exec(ws("msg","chatroom").sendText("""{"type": "SAY","nick": "#{nickname}","room": "#{channel}","msg": "Hello"}""")
					.await(30)(ws.checkTextMessage("SAY").check(
									jsonPath("$.type").is("SAY"),
									jsonPath("$.data.msg").is("Hello"),
									jsonPath("$.data.nick").is("#{nickname}")
								),
								ws.checkTextMessage("CONNECT").check(jsonPath("$.type").is("CONNECT")),
								ws.checkTextMessage("REFRESH").check(jsonPath("$.type").is("REFRESH"))
							)
							
					)
					.pause(5)
				}
			}
			.exec(exec(ws("connect","chatroom").sendText("""{"type": "PART","nick": "#{nickname}","room": "#{channel}"}""")))
		)
	)
	setUp(scn.inject(/*atOnceUsers(1),*/rampUsers(60).during(60))).protocols(httpProtocol)
}
