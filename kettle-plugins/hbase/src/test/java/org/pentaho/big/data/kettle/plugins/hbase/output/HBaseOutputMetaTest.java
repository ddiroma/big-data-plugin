/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.big.data.kettle.plugins.hbase.output;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.pentaho.big.data.kettle.plugins.hbase.LogInjector;
import org.pentaho.big.data.kettle.plugins.hbase.MappingDefinition;
import org.pentaho.big.data.kettle.plugins.hbase.NamedClusterLoadSaveUtil;
import org.pentaho.big.data.kettle.plugins.hbase.ServiceStatus;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.logging.KettleLogStore;
import org.pentaho.di.core.logging.LoggingBuffer;
import org.pentaho.di.core.variables.Variables;
import org.pentaho.di.repository.ObjectId;
import org.pentaho.di.repository.Repository;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.steps.loadsave.MemoryRepository;
import org.pentaho.hadoop.shim.api.cluster.ClusterInitializationException;
import org.pentaho.hadoop.shim.api.cluster.NamedCluster;
import org.pentaho.hadoop.shim.api.cluster.NamedClusterService;
import org.pentaho.hadoop.shim.api.cluster.NamedClusterServiceLocator;
import org.pentaho.hadoop.shim.api.hbase.HBaseService;
import org.pentaho.hadoop.shim.api.hbase.mapping.Mapping;
import org.pentaho.hadoop.shim.api.hbase.mapping.MappingFactory;
import org.pentaho.metastore.api.IMetaStore;
import org.pentaho.metastore.locator.api.MetastoreLocator;
import org.pentaho.runtime.test.RuntimeTester;
import org.pentaho.runtime.test.action.RuntimeTestActionService;

import javax.imageio.metadata.IIOMetadataNode;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith( MockitoJUnitRunner.class )
public class HBaseOutputMetaTest {

  @Mock NamedClusterService namedClusterService;
  @Mock NamedClusterServiceLocator namedClusterServiceLocator;
  @Mock RuntimeTestActionService runtimeTestActionService;
  @Mock RuntimeTester runtimeTester;
  @Mock NamedClusterLoadSaveUtil namedClusterLoadSaveUtil;
  @Mock NamedCluster namedCluster;
  @Mock MetastoreLocator metastoreLocatorOsgi;

  @Mock Repository rep;
  @Mock IMetaStore metaStore;
  @Mock ObjectId id_step;
  @Mock HBaseService hBaseService;
  @Mock MappingDefinition mappingDefinition;

  List<DatabaseMeta> databases = new ArrayList<>();

  @InjectMocks HBaseOutputMeta hBaseOutputMeta;

  @Test
  public void testReadRepSetsNamedCluster() throws Exception {
    when( namedClusterLoadSaveUtil.loadClusterConfig( any(), any(), any(), any(), any(), any() ) )
      .thenReturn( namedCluster );
    when( namedClusterServiceLocator.getService( namedCluster, HBaseService.class, null ) ).thenReturn( hBaseService );
    when( hBaseService.getMappingFactory() )
      .thenReturn( mock( MappingFactory.class ) );
    Mapping mapping = mock( Mapping.class );
    when( mapping.readRep( rep, id_step ) ).thenReturn( true );
    when( hBaseService.getMappingFactory().createMapping() ).thenReturn( mapping );

    hBaseOutputMeta.readRep( rep, metaStore, id_step, databases );
    assertThat( hBaseOutputMeta.getNamedCluster(), is( namedCluster ) );
    assertThat( hBaseOutputMeta.getMapping(), is( mapping ) );
  }

  /**
   * actual for bug BACKLOG-9529
   */
  @Test
  public void testLogSuccessfulForGetXml() throws Exception {
    HBaseOutputMeta hBaseOutputMetaSpy = Mockito.spy( this.hBaseOutputMeta );
    Mockito.doThrow( new KettleException( "Unexpected error occured" ) ).when( hBaseOutputMetaSpy )
      .applyInjection( any() );

    LoggingBuffer loggingBuffer = LogInjector.setMockForLoggingBuffer();
    hBaseOutputMetaSpy.getXML();
    verify( loggingBuffer, atLeast( 1 ) ).addLogggingEvent( any() );
  }

  /**
   * actual for bug BACKLOG-9629
   */
  @Test
  public void testApplyInjectionDefinitionExists() throws Exception {
    HBaseOutputMeta hBaseOutputMetaSpy = Mockito.spy( this.hBaseOutputMeta );
    when( namedClusterServiceLocator.getService( namedCluster, HBaseService.class, null ) ).thenReturn( hBaseService );
    hBaseOutputMetaSpy.setMappingDefinition( mappingDefinition );
    hBaseOutputMetaSpy.setNamedCluster( namedCluster );
    Mockito.doReturn( null ).when( hBaseOutputMetaSpy ).getMapping( any(), any() );

    hBaseOutputMetaSpy.getXML();
    verify( hBaseOutputMetaSpy, times( 1 ) ).setMapping( any() );
  }

  /**
   * actual for bug BACKLOG-9629
   */
  @Test
  public void testApplyInjectionDefinitionNull() throws Exception {
    HBaseOutputMeta hBaseOutputMetaSpy = Mockito.spy( this.hBaseOutputMeta );
    hBaseOutputMetaSpy.setMappingDefinition( null );
    hBaseOutputMetaSpy.setNamedCluster( namedCluster );

    hBaseOutputMetaSpy.getXML();
    verify( hBaseOutputMetaSpy, times( 0 ) ).getMapping( any(), any() );
    verify( hBaseOutputMetaSpy, times( 0 ) ).setMapping( any() );
  }

  @Test
  public void testLoadXmlDoesntBubleUpException() throws Exception {
    KettleLogStore.init();
    ClusterInitializationException exception = new ClusterInitializationException( new Exception() );
    hBaseOutputMeta.setNamedCluster( namedCluster );
    when( namedClusterServiceLocator.getService( namedCluster, HBaseService.class, null ) ).thenThrow( exception );
    when( namedClusterLoadSaveUtil.loadClusterConfig( any(), any(), any(), any(), any(), any() ) )
      .thenReturn( namedCluster );

    IIOMetadataNode node = new IIOMetadataNode();
    IIOMetadataNode child = new IIOMetadataNode( "disable_wal" );
    IIOMetadataNode grandChild = new IIOMetadataNode();
    grandChild.setNodeValue( "N" );
    child.appendChild( grandChild );
    node.appendChild( child );

    hBaseOutputMeta.loadXML( node, new ArrayList<>(), metaStore );

    ServiceStatus serviceStatus = hBaseOutputMeta.getServiceStatus();
    assertNotNull( serviceStatus );
    assertFalse( serviceStatus.isOk() );
    assertEquals( exception, serviceStatus.getException() );
  }

  @Test
  public void testLoadXmlServiceStatusOk() throws Exception {
    KettleLogStore.init();
    hBaseOutputMeta.setNamedCluster( namedCluster );
    when( namedClusterServiceLocator.getService( namedCluster, HBaseService.class, null ) ).thenReturn( hBaseService );
    when( namedClusterLoadSaveUtil.loadClusterConfig( any(), any(), any(), any(), any(), any() ) )
      .thenReturn( namedCluster );

    IIOMetadataNode node = new IIOMetadataNode();
    IIOMetadataNode child = new IIOMetadataNode( "disable_wal" );
    IIOMetadataNode grandChild = new IIOMetadataNode();
    grandChild.setNodeValue( "N" );
    child.appendChild( grandChild );
    node.appendChild( child );

    hBaseOutputMeta.loadXML( node, new ArrayList<>(), metaStore );

    ServiceStatus serviceStatus = hBaseOutputMeta.getServiceStatus();
    assertNotNull( serviceStatus );
    assertTrue( serviceStatus.isOk() );
  }

  @Test
  public void testReadRepDoesntBubleUpException() throws Exception {
    KettleLogStore.init();
    ClusterInitializationException exception = new ClusterInitializationException( new Exception() );
    hBaseOutputMeta.setNamedCluster( namedCluster );
    when( namedClusterServiceLocator.getService( namedCluster, HBaseService.class, null ) ).thenThrow( exception );
    when( namedClusterLoadSaveUtil.loadClusterConfig( any(), any(), any(), any(), any(), any() ) )
      .thenReturn( namedCluster );

    hBaseOutputMeta.readRep( new MemoryRepository(), metaStore, mock( ObjectId.class ), new ArrayList<>() );

    ServiceStatus serviceStatus = hBaseOutputMeta.getServiceStatus();
    assertNotNull( serviceStatus );
    assertFalse( serviceStatus.isOk() );
    assertEquals( exception, serviceStatus.getException() );
  }

  @Test
  public void testReadRepServiceStatusOk() throws Exception {
    KettleLogStore.init();
    hBaseOutputMeta.setNamedCluster( namedCluster );
    when( namedClusterServiceLocator.getService( namedCluster, HBaseService.class, null ) ).thenReturn( hBaseService );
    when( namedClusterLoadSaveUtil.loadClusterConfig( any(), any(), any(), any(), any(), any() ) )
      .thenReturn( namedCluster );

    hBaseOutputMeta.readRep( new MemoryRepository(), metaStore, mock( ObjectId.class ), new ArrayList<>() );

    ServiceStatus serviceStatus = hBaseOutputMeta.getServiceStatus();
    assertNotNull( serviceStatus );
    assertTrue( serviceStatus.isOk() );
  }

  @Test
  public void testInjectWithEmbeddedMetastoreProviderKey() throws Exception {
    KettleLogStore.init();
    hBaseOutputMeta.setNamedCluster( namedCluster );
    when( namedCluster.getName() ).thenReturn( "ClusterName" );
    NamedCluster embeddedNamedCluster = mock( NamedCluster.class );
    when( embeddedNamedCluster.getShimIdentifier() ).thenReturn( "shim" );
    StepMeta mockStepMeta = mock( StepMeta.class );
    TransMeta mockTransMeta = mock( TransMeta.class );
    when( mockTransMeta.getEmbeddedMetastoreProviderKey() ).thenReturn( "key" );
    hBaseOutputMeta.setParentStepMeta( mockStepMeta );
    when( mockStepMeta.getParentTransMeta() ).thenReturn( mockTransMeta );
    when( metastoreLocatorOsgi.getExplicitMetastore( "key" ) ).thenReturn( metaStore );
    when( namedClusterService.getNamedClusterByName( "ClusterName", metaStore ) ).thenReturn( embeddedNamedCluster );

    hBaseOutputMeta.applyInjection( new Variables() );
    assertEquals( embeddedNamedCluster, hBaseOutputMeta.getNamedCluster() );
  }
}
