//package dprsnn.com.paymentsRegisters.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.Arrays;
//import java.util.List;
//
//@Component
//public class IpAccessFilter extends OncePerRequestFilter {
//
//    @Value("${security.allowed-ips}")
//    private String allowedIpsRaw;
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String ip = getClientIp(request);
//        System.out.println("🔍 Запит з IP: " + ip);
//
//        List<String> allowedIps = Arrays.asList(allowedIpsRaw.split("\\s*,\\s*"));
//
//        if (!allowedIps.contains(ip)) {
//            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
//            response.setContentType("text/plain;charset=UTF-8");
//            response.getWriter().write("⛔ Вам заборонено доступ");
//            return;
//        }
//
//        filterChain.doFilter(request, response);
//    }
//
//    private String getClientIp(HttpServletRequest request) {
//        String xfHeader = request.getHeader("X-Forwarded-For");
//        if (xfHeader != null && !xfHeader.isEmpty()) {
//            return xfHeader.split(",")[0];
//        }
//        return request.getRemoteAddr();
//    }
//}
