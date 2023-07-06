package ece448.iot_hub;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PlugsResource {
      private final PlugsModel plugs;
      public PlugsResource(PlugsModel plugs) throws Exception {
      this.plugs = plugs;
      }

      //list plugs
      @GetMapping("/api/plugs")
      public Collection<Object> plugs() throws Exception {
            ArrayList<Object> ret = new ArrayList<>();
            for (String plug: plugs.getPlugs()) {
                  ret.add(makePlug(plug));
            }
            logger.info("Plugs: {}", ret);
            return ret; 
      }

      protected Object makePlug(String plugName) {
            HashMap<String, Object> plugDetails = new HashMap<>();
            plugDetails.put("name",plugName);
            plugDetails.put("state", plugs.plugState(plugName));
            return plugDetails;
      }

      @GetMapping("/api/plugs/{plugName:.+}")
            public Object getPlug(
            @PathVariable("plugName") String plugName,
            @RequestParam(value = "action", required = false) String action) throws Exception {
                  if(action==null){
                        Object ret=makePlug(plugName);
                        logger.info("PlugName {}:{}",plugName,ret);
                        return ret;
                  }

                  plugs.updatePlugs(plugName, action);
                  return makePlug(plugName);
      }
               
      private static final Logger logger =LoggerFactory.getLogger(PlugsResource.class);
}
