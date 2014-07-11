package org.springframework.platform.netflix.zuul;

import com.netflix.zuul.http.ZuulServlet;
import org.springframework.platform.util.AbstractServletWrappingController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: spencergibb
 * Date: 4/24/14
 * Time: 9:12 PM
 */
@Controller
public class ZuulController extends AbstractServletWrappingController {

    public ZuulController() {
        super(ZuulServlet.class, "zuulServlet");
    }

    @RequestMapping("/**")
    public ModelAndView handle(HttpServletRequest request, HttpServletResponse response) throws Exception {
        return this.controller.handleRequest(request, response);
    }
}
