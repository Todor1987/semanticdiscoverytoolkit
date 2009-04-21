/*
    Copyright 2009 Semantic Discovery, Inc. (www.semanticdiscovery.com)

    This file is part of the Semantic Discovery Toolkit.

    The Semantic Discovery Toolkit is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    The Semantic Discovery Toolkit is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with The Semantic Discovery Toolkit.  If not, see <http://www.gnu.org/licenses/>.
*/
package org.sd.cluster.job;


import org.sd.cluster.config.ClusterContext;

/**
 * A partition function that returns each partition in 'round robin' order.
 * <p>
 * @author Spence Koehler
 */
public class RoundRobinPartitionFunction extends AbstractPartitionFunction {
  
  private int nextPartition = 0;
  private final Object nextMutex = new Object();

  public RoundRobinPartitionFunction() {
    super();
  }

  public RoundRobinPartitionFunction(ClusterContext clusterContext, String groupName) {
    super(clusterContext, groupName);
  }

  /**
   * Get the key's partition as key mod numPartitions.
   *
   * @return the partition for the given key: a number between 0 and numPartitions.
   */
  protected final int getPartition(long key, int numPartitions) {
    int result = 0;

    synchronized (nextMutex) {
      result = nextPartition;
      nextPartition = (nextPartition + 1) % numPartitions;
    }
    
    return result;
  }
}
