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

package org.apache.hadoop.yarn.api.records.impl.pb;


import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.proto.YarnProtos.ResourceBlacklistRequestProto;
import org.apache.hadoop.yarn.proto.YarnProtos.ResourceProto;
import org.apache.hadoop.yarn.proto.YarnProtos.ResourceProtoOrBuilder;

@Private
@Unstable
/*
 * @author Tim and lism
 * @date 2013-10-15
 * @description Add GPU resource and related functions
 */
public class ResourcePBImpl extends Resource {
  ResourceProto proto = ResourceProto.getDefaultInstance();
  ResourceProto.Builder builder = null;
  boolean viaProto = false;
  
  List<Integer> gpuId = null;
  
  public ResourcePBImpl() {
    builder = ResourceProto.newBuilder();
  }

  public ResourcePBImpl(ResourceProto proto) {
    this.proto = proto;
    viaProto = true;
  }
  
  public ResourceProto getProto() {
	mergeLocalToProto();
    proto = viaProto ? proto : builder.build();
    viaProto = true;
    return proto;
  }
  
  private void maybeInitBuilder() {
	    if (viaProto || builder == null) {
	      builder = ResourceProto.newBuilder(proto);
	    }
	    viaProto = false;
	  }
  
  /*add by lism :  gpu id*/
  
  private void mergeLocalToProto() {
    if (viaProto) {
      maybeInitBuilder();
    }
    mergeLocalToBuilder();
    proto = builder.build();
    viaProto = true;
  }
  
  private void mergeLocalToBuilder() {
	    if (this.gpuId != null) 
	      addGPUIdToProto();
	  }
	  
  private void addGPUIdToProto() {
	    maybeInitBuilder();
	    builder.clearGpuId();
	    if (this.gpuId == null) { 
	      return;
	    }
	    builder.addAllGpuId(this.gpuId);
}
  
  private void initGPUId() {
		 if(this.gpuId != null) {
			 return;
		 }
		 ResourceProtoOrBuilder p = viaProto ? proto : builder;
		 List<Integer> list = p.getGpuIdList();
		 this.gpuId = new ArrayList<Integer>();
		 this.gpuId.addAll(list);
	}

	public List<Integer> getGPUId() {
		 initGPUId();
		 return this.gpuId;
	}

	public void setGPUId(List<Integer> gpuID) {
		 if (gpuID == null) {
			 if(this.gpuId != null) {
				 this.gpuId.clear();
			 }
			 return;
		 }
		 initGPUId();
		 this.gpuId.clear();
		 this.gpuId.addAll(gpuID);
	}

  @Override
  public int getMemory() {
    ResourceProtoOrBuilder p = viaProto ? proto : builder;
    return (p.getMemory());
  }

  @Override
  public void setMemory(int memory) {
    maybeInitBuilder();
    builder.setMemory((memory));
  }

  @Override
  public int getVirtualCores() {
    ResourceProtoOrBuilder p = viaProto ? proto : builder;
    return (p.getVirtualCores());
  }

  @Override
  public void setVirtualCores(int vCores) {
    maybeInitBuilder();
    builder.setVirtualCores((vCores));
  }
  
  /*
   * @author: Shengmei Li
   * Add GPU info
   */
  public int getGPUCores(){
	  ResourceProtoOrBuilder p = viaProto ? proto : builder;
	  return (p.getGpuCores());
  }
  public void setGPUCores(int gCores) {
	    maybeInitBuilder();
	    builder.setGpuCores(gCores);
	  }

  
/*
 * add by lism: gpuid
 * */


  @Override
  public int compareTo(Resource other) {
    int diff = this.getMemory() - other.getMemory();
    if (diff == 0) {
      diff = this.getVirtualCores() - other.getVirtualCores();
      if( diff == 0)
      {
    	  diff = this.getGPUCores() - other.getGPUCores();
      }
    }
    return diff;
  }
  
  
}  
