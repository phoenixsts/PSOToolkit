/*
 * com.percussion.pso.sandbox PSOFolderTools.java
 * 
 * @copyright 2006 Percussion Software, Inc. All rights reserved.
 * See license.txt for detailed restrictions. 
 * @author MikeStarck
 *
 */
package com.percussion.pso.jexl;

import java.rmi.RemoteException;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.extension.IPSJexlExpression;
import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.IPSJexlParam;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSJexlUtilBase; 
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.services.content.data.PSItemSummary;
import com.percussion.services.content.data.PSContentTypeSummary;
import com.percussion.webservices.content.PSContentWsLocator;

/**
 * 
 *
 * @author MikeStarck
 *
 */
public class PSORelationshipTools extends PSJexlUtilBase implements IPSJexlExpression 
{
   /**
    * Logger for this class
    */
   private static final Log log = LogFactory.getLog(PSORelationshipTools.class);
   
   
   /**
    * 
    */
   public PSORelationshipTools()
   {
      super();
   }
   
   @IPSJexlMethod(description="get the dependents of this item of a certain content type", 
         params={
		@IPSJexlParam(name="itemId", description="the item GUID"),
		@IPSJexlParam(name="contenttypeid", type="String", description="the name of the content type we are testing for")})
   public List<PSItemSummary> getRelationships(IPSGuid itemId, String contenttypename) 
   throws PSErrorException, PSExtensionProcessingException   
   {
      String userName = "fred";
      String errmsg; 
      if(itemId == null || contenttypename == null)
      {
         errmsg = "No dependents found for null guid or null contenttypename"; 
         log.error(errmsg); 
         throw new PSExtensionProcessingException(0, errmsg); 
      }
      IPSContentWs cws = PSContentWsLocator.getContentWebservice();
      IPSGuid contenttypeid;
      List<PSContentTypeSummary> ctypes = null;
      try
      {
         ctypes = cws.loadContentTypes(contenttypename);
         contenttypeid = ctypes.get(0).getGuid();
      } catch (RemoteException e1)
      {
         log.error("Cannot load content types", e1); 
         throw new PSExtensionProcessingException(PSORelationshipTools.class.getName(), e1);
      } 
      
      List<PSItemSummary> dependents = null; 
      try
      {
         PSRelationshipFilter filter = new PSRelationshipFilter();
         filter.setDependentContentTypeId(contenttypeid.longValue()); 
         dependents = cws.findDependents(
				itemId, 
				filter,
                false,
                userName);
      } catch (Exception e)
      {
        log.error("Unexpected exception " + e.getMessage(), e );
        throw new PSExtensionProcessingException(this.getClass().getCanonicalName(), e); 
      } 
      if(dependents.isEmpty())
      {
         errmsg = "cannot find dependents for " + itemId; 
         log.error(errmsg); 
         throw new PSExtensionProcessingException(0, errmsg); 
      }
      return dependents;
   }
   
}