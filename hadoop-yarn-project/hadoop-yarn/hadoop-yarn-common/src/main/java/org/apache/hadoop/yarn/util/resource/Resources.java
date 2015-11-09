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

package org.apache.hadoop.yarn.util.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience.Private;
import org.apache.hadoop.classification.InterfaceStability.Unstable;
import org.apache.hadoop.yarn.api.records.Resource;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;
import org.apache.hadoop.yarn.util.Records;
import org.apache.hadoop.yarn.exceptions.YarnRuntimeException;

@Private
@Unstable
public class Resources {
  private static final Log LOG = LogFactory.getLog(Resources.class);
  // Java doesn't have const :(
  private static final Resource NONE = new Resource() {

    @Override
    public int getMemory() {
      return 0;
    }

    @Override
    public void setMemory(int memory) {
      throw new RuntimeException("NONE cannot be modified!");
    }

    @Override
    public int getVirtualCores() {
      return 0;
    }

    @Override
    public void setVirtualCores(int cores) {
      throw new RuntimeException("NONE cannot be modified!");
    }

    @Override
    public int compareTo(Resource o) {
      int diff = 0 - o.getMemory();
      if (diff == 0) {
        diff = 0 - o.getVirtualCores();
        if(diff == 0)
        	diff = 0 - o.getGPUCores();
      }
      return diff;
    }

	@Override
	public int getGPUCores() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setGPUCores(int gCores) {
		// TODO Auto-generated method stub
		throw new RuntimeException("NONE cannot be modified!");
		
	}
	
	public List<Integer> getGPUId() {
		return null;
	}
	
	public void setGPUId(List<Integer> gpuId){
		throw new RuntimeException("NONE cannot be modified!");
	}

  };
  
  private static final Resource UNBOUNDED = new Resource() {

    @Override
    public int getMemory() {
      return Integer.MAX_VALUE;
    }

    @Override
    public void setMemory(int memory) {
      throw new RuntimeException("UNBOUND cannot be modified!");
    }

    @Override
    public int getVirtualCores() {
      return Integer.MAX_VALUE;
    }

    @Override
    public void setVirtualCores(int cores) {
      throw new RuntimeException("UNBOUND cannot be modified!");
    }

    @Override
    public int compareTo(Resource o) {
      int diff = 0 - o.getMemory();
      if (diff == 0) {
        diff = 0 - o.getVirtualCores();
        if(diff == 0)
        	diff = 0 - o.getGPUCores();
      }
      return diff;
    }

	@Override
	public int getGPUCores() {
		// TODO Auto-generated method stub
		return Integer.MAX_VALUE;
	}

	@Override
	public void setGPUCores(int gCores) {
		// TODO Auto-generated method stub
		throw new RuntimeException("UNBOUND cannot be modified!");
	}
	
	public List<Integer> getGPUId() {
		List<Integer> gpuId = new ArrayList<Integer>();
	    for(int i=0;i<Integer.MAX_VALUE;i++)
	    	gpuId.add(i);
		return gpuId;
	}
	
	public void setGPUId(List<Integer> gpuId){
		throw new RuntimeException("UNBOUND cannot be modified!");
	}
    
  };

  public static Resource createResource(int memory) {    return createResource(memory, (memory > 0) ? 1 : 0);
  }

  public static Resource createResource(int memory, int cores) {
    Resource resource = Records.newRecord(Resource.class);
    resource.setMemory(memory);
    resource.setVirtualCores(cores);
    resource.setGPUCores(0);
    resource.setGPUId(null);
    return resource;
  }
  
  /* lism:this method is called by FifoScheduler.reinitialize(), the request gpu ids are not certain.
   * so we needn't assign the requested gpu id
   * */
  public static Resource createResource(int memory, int cores, int gcores) {
	Resource resource = Records.newRecord(Resource.class);
	resource.setMemory(memory);
	resource.setVirtualCores(cores);
	resource.setGPUCores(gcores);   
	return resource;
  }
  
  public static Resource createResource(int memory, int cores, int gcores, List<Integer> gpuId) {
	Resource resource = Records.newRecord(Resource.class);
	resource.setMemory(memory);
	resource.setVirtualCores(cores);
	resource.setGPUCores(gcores);
	resource.setGPUId(gpuId);
	return resource;
  }
  
  public static Resource none() {
    return NONE;
  }
  
  public static Resource unbounded() {
    return UNBOUNDED;
  }

  public static Resource clone(Resource res) {
    return createResource(res.getMemory(), res.getVirtualCores(),res.getGPUCores(),res.getGPUId());
  }

  public static Resource addTo(Resource lhs, Resource rhs) {
    lhs.setMemory(lhs.getMemory() + rhs.getMemory());
    lhs.setVirtualCores(lhs.getVirtualCores() + rhs.getVirtualCores());
//    LOG.info("to set GPU Cores: " + (lhs.getGPUCores() + rhs.getGPUCores()));
    lhs.setGPUCores(lhs.getGPUCores() + rhs.getGPUCores());
//    LOG.info("remaining GPUs: " + rhs.getGPUCores());
    if(rhs.getGPUCores()>0 && rhs.getGPUId().size() > 0)
    {
//      for(Integer i : rhs.getGPUId()) {
//        LOG.info(i);
//      }
    	if(lhs.getGPUId()==null||lhs.getGPUId().size()<1)
    	{
    		List<Integer> lhsGpuId = new ArrayList<Integer>();
    		lhsGpuId.add(rhs.getGPUId().get(0));
    		lhs.setGPUId(lhsGpuId);
    	}
    	else
    		lhs.getGPUId().add(rhs.getGPUId().get(0));
     }
    return lhs;
  }

  public static Resource add(Resource lhs, Resource rhs) {
    return addTo(clone(lhs), rhs);
  }

  public static Resource subtractFrom(Resource lhs, Resource rhs) {
    lhs.setMemory(lhs.getMemory() - rhs.getMemory());
    lhs.setVirtualCores(lhs.getVirtualCores() - rhs.getVirtualCores());
    lhs.setGPUCores(lhs.getGPUCores() - rhs.getGPUCores());
//    LOG.info("substract from " + lhs.getGPUCores() + " GPUs: ");
//    for(Integer i : lhs.getGPUId()) {
//      LOG.info(i);
//    }
//    // TO fix!!! seems to substract more than offered! but the gpuid list is correctly cleared
//    LOG.info("to substract " + rhs.getGPUCores() + " GPUs: ");
    for(Integer i : rhs.getGPUId()) {
//      LOG.info("remove " + i + " GPU from cluster available GPUs");
      lhs.getGPUId().remove(i);
    }

    return lhs;
  }

  public static Resource subtract(Resource lhs, Resource rhs) {
    return subtractFrom(clone(lhs), rhs);
  }

  public static Resource negate(Resource resource) {
    return subtract(NONE, resource);
  }

  public static Resource multiplyTo(Resource lhs, double by) {
    lhs.setMemory((int)(lhs.getMemory() * by));
    lhs.setVirtualCores((int)(lhs.getVirtualCores() * by));
    lhs.setGPUCores((int)(lhs.getGPUCores() * by));
    return lhs;
  }

  public static Resource multiply(Resource lhs, double by) {
    return multiplyTo(clone(lhs), by);
  }
  
  public static Resource multiplyAndNormalizeUp(
      ResourceCalculator calculator,Resource lhs, double by, Resource factor) {
    return calculator.multiplyAndNormalizeUp(lhs, by, factor);
  }
  
  public static Resource multiplyAndNormalizeDown(
      ResourceCalculator calculator,Resource lhs, double by, Resource factor) {
    return calculator.multiplyAndNormalizeDown(lhs, by, factor);
  }
  
  public static Resource multiplyAndRoundDown(Resource lhs, double by) {
    Resource out = clone(lhs);
    out.setMemory((int)(lhs.getMemory() * by));
    out.setVirtualCores((int)(lhs.getVirtualCores() * by));
    out.setGPUCores((int)(lhs.getGPUCores() * by));
    return out;
  }
  
  public static Resource normalize(
      ResourceCalculator calculator, Resource lhs, Resource min,
      Resource max, Resource increment) {
//    LOG.info("resource before normalized: Mem: " + lhs.getMemory() + ", CPU: " + lhs.getVirtualCores() + ", GPU: " + lhs.getGPUCores());
    return calculator.normalize(lhs, min, max, increment);
  }
  
  public static Resource roundUp(
      ResourceCalculator calculator, Resource lhs, Resource factor) {
    return calculator.roundUp(lhs, factor);
  }
  
  public static Resource roundDown(
      ResourceCalculator calculator, Resource lhs, Resource factor) {
    return calculator.roundDown(lhs, factor);
  }
  
  public static float ratio(
      ResourceCalculator resourceCalculator, Resource lhs, Resource rhs) {
    return resourceCalculator.ratio(lhs, rhs);
  }
  
  public static float divide(
      ResourceCalculator resourceCalculator,
      Resource clusterResource, Resource lhs, Resource rhs) {
    return resourceCalculator.divide(clusterResource, lhs, rhs);
  }
  
  public static Resource divideAndCeil(
      ResourceCalculator resourceCalculator, Resource lhs, int rhs) {
    return resourceCalculator.divideAndCeil(lhs, rhs);
  }
  
  public static boolean equals(Resource lhs, Resource rhs) {
    return lhs.equals(rhs);
  }

  public static boolean lessThan(
      ResourceCalculator resourceCalculator, 
      Resource clusterResource,
      Resource lhs, Resource rhs) {
    return (resourceCalculator.compare(clusterResource, lhs, rhs) < 0);
  }

  public static boolean lessThanOrEqual(
      ResourceCalculator resourceCalculator, 
      Resource clusterResource,
      Resource lhs, Resource rhs) {
    return (resourceCalculator.compare(clusterResource, lhs, rhs) <= 0);
  }

  public static boolean greaterThan(
      ResourceCalculator resourceCalculator,
      Resource clusterResource,
      Resource lhs, Resource rhs) {
    return resourceCalculator.compare(clusterResource, lhs, rhs) > 0;
  }

  public static boolean greaterThanOrEqual(
      ResourceCalculator resourceCalculator, 
      Resource clusterResource,
      Resource lhs, Resource rhs) {
    return resourceCalculator.compare(clusterResource, lhs, rhs) >= 0;
  }
  
  public static Resource min(
      ResourceCalculator resourceCalculator, 
      Resource clusterResource,
      Resource lhs, Resource rhs) {
    return resourceCalculator.compare(clusterResource, lhs, rhs) <= 0 ? lhs : rhs;
  }

  public static Resource max(
      ResourceCalculator resourceCalculator, 
      Resource clusterResource,
      Resource lhs, Resource rhs) {
    return resourceCalculator.compare(clusterResource, lhs, rhs) >= 0 ? lhs : rhs;
  }
  
  public static boolean fitsIn(Resource smaller, Resource bigger) {
    return smaller.getMemory() <= bigger.getMemory() &&
        smaller.getVirtualCores() <= bigger.getVirtualCores();
  }
  
  public static Resource componentwiseMin(Resource lhs, Resource rhs) {
    return createResource(Math.min(lhs.getMemory(), rhs.getMemory()),
        Math.min(lhs.getVirtualCores(), rhs.getVirtualCores()));
  }
}
