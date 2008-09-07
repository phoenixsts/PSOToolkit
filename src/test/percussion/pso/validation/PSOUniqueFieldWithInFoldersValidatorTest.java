package test.percussion.pso.validation;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.junit.Before;
import org.junit.Test;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.pso.utils.PSONodeCataloger;
import com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.content.IPSContentWs;

public class PSOUniqueFieldWithInFoldersValidatorTest {

    private static Log log = LogFactory.getLog(PSOUniqueFieldWithInFoldersValidatorTest.class);
    
    TestablePSOUniqueFieldValidator validator;
    Mockery context;
    PSONodeCataloger nodeCataloger; 
    
    @Before
    public void setUp() throws Exception {
        validator = new TestablePSOUniqueFieldValidator();
        context = new Mockery(){{setImposteriser(ClassImposteriser.INSTANCE);}};
        nodeCataloger = context.mock(PSONodeCataloger.class);
        validator.setNodeCataloger(nodeCataloger); 
    }
    
    @Test
    public void testGetQueryForValueInFolder() throws Exception {
        String expected = "select rx:sys_contentid, rx:filename" +
        " from nt:base " +
        "where " +
        "rx:filename = \'test\' " +
        "and " +
        "jcr:path = \'//Sites/Blah\'"; 
        String actual = validator.getQueryForValueInFolder("filename", "test", "//Sites/Blah", "nt:base");
        assertEquals(expected, actual);
    }
    
    @Test
    public void testGetQueryForValueInFolders() throws Exception {
        String expected = 
            "select rx:sys_contentid, rx:filename " +
            "from nt:base " +
            "where " +
            "rx:sys_contentid != 2000 " +
            "and " +
            "rx:filename = 'test' " +
            "and " +
            "(jcr:path = '//Sites/A' or jcr:path = '//Sites/B')";
            String actual = validator.getQueryForValueInFolders(
                    2000,"filename", "test", new String[] {"//Sites/A","//Sites/B"}, "nt:base");
        assertEquals(expected, actual);
    }

    @Test
    public void testMakeTypeList()
    {
        try
      {
         context.checking(new Expectations(){{
              one(nodeCataloger).getContentTypeNamesWithField("fld");
              will(returnValue(Arrays.asList(new String[]{"x","y","z"})));
           }});
         
         String str = validator.makeTypeList("fld");
         assertNotNull(str); 
         String expected = "x, y, z";
         
         assertEquals(expected, str);
         log.info("type list is " + str); 
         context.assertIsSatisfied(); 
      } catch (RepositoryException ex)
      {
         log.error("Unexpected Exception " + ex,ex);
      }
    }
    
    @Test
    public void testGetFolderId()
    {
       final IPSRequestContext req = context.mock(IPSRequestContext.class); 
       
       final String psredirect = "http://base123?ps1=2&sys_folderid=1234&foo=bar"; 
       
       context.checking(new Expectations(){{
          one(req).getParameter("psredirect"); 
          will(returnValue(psredirect));
       }});
       
       Integer fid = validator.getFolderId(req);
       assertEquals(1234,fid); 
       context.assertIsSatisfied(); 
    }
    
    @Test
    public void testIsFieldValueUniqueInFolder()
    {
       final IPSGuid folderGuid = context.mock(IPSGuid.class); 
       final IPSGuidManager gmgr = context.mock(IPSGuidManager.class); 
       final IPSContentWs cws = context.mock(IPSContentWs.class); 
       final PSFolder folder = context.mock(PSFolder.class); 
       final List<PSFolder> folderList = Arrays.asList(new PSFolder[]{folder});
       final IPSContentMgr cmgr = context.mock(IPSContentMgr.class);
       final Query q = context.mock(Query.class);
       final QueryResult qres = context.mock(QueryResult.class);
       final RowIterator rows = context.mock(RowIterator.class); 
       
       
       try
      {
         validator.setContentManager(cmgr);
         validator.setContentWs(cws);
         validator.setGuidManager(gmgr); 
         
         context.checking(new Expectations(){{
             one(gmgr).makeGuid(with(any(PSLocator.class)));
             will(returnValue(folderGuid)); 
             one(cws).loadFolders(Arrays.asList(new IPSGuid[]{folderGuid}));
             will(returnValue(folderList));
             one(folder).getFolderPath();
             will(returnValue("/foo/bar/baz"));
             one(cmgr).createQuery(with(any(String.class)), with(any(String.class)));
             will(returnValue(q)); 
             one(cmgr).executeQuery(q, -1, null, null); 
             will(returnValue(qres)); 
             one(qres).getRows();
             will(returnValue(rows));
             one(rows).getSize();
             will(returnValue(0L)); 
          }});
         
         boolean val = validator.isFieldValueUniqueInFolder(123, "rx:field", "foo", "rx:type, rx:anothertype"); 
         assertTrue(val);  
         context.assertIsSatisfied();
         
      } catch (Exception ex)
      {
         log.error("Unexpected Exception " + ex,ex);
         fail("Exception");
      }
       
    }
    
    @Test
    public void testIsFieldValueUniqueInFolderForExistingItem()
    {
       final IPSGuid guid = context.mock(IPSGuid.class);
       final IPSGuidManager gmgr = context.mock(IPSGuidManager.class); 
       final IPSContentWs cws = context.mock(IPSContentWs.class); 
       final IPSContentMgr cmgr = context.mock(IPSContentMgr.class);
       final Query q = context.mock(Query.class);
       final QueryResult qres = context.mock(QueryResult.class);
       final RowIterator rows = context.mock(RowIterator.class); 
       
       
       try
      {
         validator.setContentManager(cmgr);
         validator.setContentWs(cws);
         validator.setGuidManager(gmgr); 
         
         context.checking(new Expectations(){{
             one(gmgr).makeGuid(with(any(PSLocator.class)));
             will(returnValue(guid)); 
             one(cws).findFolderPaths(guid);
             will(returnValue(new String[]{"/foo/bar/baz"}));
             one(cmgr).createQuery(with(any(String.class)), with(any(String.class)));
             will(returnValue(q)); 
             one(cmgr).executeQuery(q, -1, null, null); 
             will(returnValue(qres)); 
             one(qres).getRows();
             will(returnValue(rows));
             one(rows).getSize();
             will(returnValue(0L)); 
          }});
         
         boolean val = validator.isFieldValueUniqueInFolderForExistingItem(123, "rx:field", "foo", "rx:type, rx:anothertype"); 
         assertTrue(val);  
         context.assertIsSatisfied();
         
      } catch (Exception ex)
      {
         log.error("Unexpected Exception " + ex,ex);
         fail("Exception");
      }
    }
    /**
     * Test class to expose protected methods. 
     * 
     *
     * @author DavidBenua
     *
     */
    private class TestablePSOUniqueFieldValidator extends PSOUniqueFieldWithInFoldersValidator
    {

      /**
       * @see com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator#makeTypeList(java.lang.String)
       */
      @Override
      public String makeTypeList(String fieldname)
            throws RepositoryException
      {         
         return super.makeTypeList(fieldname);
      }

      /**
       * @see com.percussion.pso.validation.PSOUniqueFieldWithInFoldersValidator#getFolderId(com.percussion.server.IPSRequestContext)
       */
      @Override
      public Integer getFolderId(IPSRequestContext request)
      {
         return super.getFolderId(request);
      }
       
    }
    
    
}