/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hadoop.yarn.api.records;

import org.apache.hadoop.classification.InterfaceAudience.Public;
import org.apache.hadoop.classification.InterfaceStability.Evolving;
import org.apache.hadoop.classification.InterfaceStability.Stable;
import org.apache.hadoop.yarn.api.ApplicationMasterProtocol;
import org.apache.hadoop.yarn.util.Records;

import java.util.ArrayList;
import java.util.List;

/**
 * <p><code>Resource</code> models a set of computer resources in the 
 * cluster.</p>
 * 
 * <p>Currrently it only models <em>memory</em>.</p>
 * 
 * <p>Typically, applications request <code>Resource</code> of suitable
 * capability to run their component tasks.</p>
 * 
 * @see ResourceRequest
 * @see ApplicationMasterProtocol#allocate(org.apache.hadoop.yarn.api.protocolrecords.AllocateRequest)
 */
/*
 * @author Tim
 * @date 2013-10-15
 * @description Add GPU resource and related functions
 */
@Public
@Stable
public abstract class Resource implements Comparable<Resource> {

  @Public
  @Stable
  public static Resource newInstance(int memory, int vCores) {
    Resource resource = Records.newRecord(Resource.class);
    resource.setMemory(memory);
    resource.setVirtualCores(vCores);
    resource.setGPUCores(0);
    resource.setGPUId(null);
    return resource;
  }

  public static Resource newInstance(int memory, int vCores,int gCores, List<Integer> gpuId) {
	    Resource resource = Records.newRecord(Resource.class);
	    resource.setMemory(memory);
	    resource.setVirtualCores(vCores);
	    resource.setGPUCores(gCores);
	    resource.setGPUId(gpuId);
	    return resource;
	  }
  
  public static Resource newInstance(Resource res) {
		// TODO Auto-generated method stub
	    Resource resource = Records.newRecord(Resource.class);
	    resource.setMemory(res.getMemory());
	    resource.setVirtualCores(res.getVirtualCores());
	    resource.setGPUCores(res.getGPUCores());
	    if(res.getGPUCores()==0)
	    	resource.setGPUId(null);
	    else
	    	resource.setGPUId(res.getGPUId());
	    return resource;
	}
  /**
   * Get <em>memory</em> of the resource.
   * @return <em>memory</em> of the resource
   */
  @Public
  @Stable
  public abstract int getMemory();
  
  /**
   * Set <em>memory</em> of the resource.
   * @param memory <em>memory</em> of the resource
   */
  @Public
  @Stable
  public abstract void setMemory(int memory);


  /**
   * Get <em>number of virtual cpu cores</em> of the resource.
   * 
   * We refer to <em>virtual cores</em> to clarify that these represent
   * <em>normalized</em> cores which may have a m:n relationship w.r.t
   * physical cores available on the compute nodes. Furthermore, they also 
   * represent <em>idealized</em> cores since the cluster might be composed
   * of <em>heterogenous</em> nodes.
   *   
   * @return <em>num of virtual cpu cores</em> of the resource
   */
  @Public
  @Evolving
  public abstract int getVirtualCores();
  
  /**
   * Set <em>number of virtual cpu cores</em> of the resource.
   * 
   * We refer to <em>virtual cores</em> to clarify that these represent
   * <em>normalized</em> cores which may have a m:n relationship w.r.t
   * physical cores available on the compute nodes. Furthermore, they also 
   * represent <em>idealized</em> cores since the cluster might be composed
   * of <em>heterogenous</em> nodes.
   *   
   * @param vCores <em>number of virtual cpu cores</em> of the resource
   */
  @Public
  @Evolving
  public abstract void setVirtualCores(int vCores);
  public abstract int getGPUCores();
  public abstract void setGPUCores(int gCores);
  public abstract List<Integer> getGPUId();
  public abstract void setGPUId(List<Integer> gpuId);
  //public abstract int availableGPUId(List<Integer> gpuID);
  
  
  @Override
  public int hashCode() {
    final int prime = 263167;
    int result = 3571;
    result = 939769357 + getMemory(); // prime * result = 939769357 initially
    result = prime * result + getVirtualCores();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Resource))
      return false;
    Resource other = (Resource) obj;
    if (getMemory() != other.getMemory() || 
        getVirtualCores() != other.getVirtualCores() || getGPUCores() != other.getGPUCores()) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "<memory:" + getMemory() + ", vCores:" + getVirtualCores() + ", gCores:" + getGPUCores() + 
    		", gpuId: "+ getGPUId().toString()+">";
  }

}
