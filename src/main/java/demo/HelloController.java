package demo;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller
public class HelloController {

	@Get
	String index() {
		return "Hello, world!";
	}
}
