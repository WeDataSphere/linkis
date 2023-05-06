/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.basedatamanager.server.utils;

import org.apache.linkis.basedatamanager.server.domain.UdfTreeEntity;

import java.util.ArrayList;
import java.util.List;

public class UdfTreeUtils {

  /** BuildTree 构建树形结构 */
  public List<UdfTreeEntity> udfTreeList = new ArrayList<>();

  /** 构造方法 */
  public UdfTreeUtils(List<UdfTreeEntity> udfTreeList) {
    this.udfTreeList = udfTreeList;
  }

  /**
   * 获取需构建的所有根节点（顶级节点）
   *
   * @return 所有根节点List集合
   */
  public List<UdfTreeEntity> getRootNode() {
    // 保存所有根节点（所有根节点的数据）
    List<UdfTreeEntity> rootudfTreeList = new ArrayList<>();
    // UdfTreeEntity：查询出的每一条数据（节点）
    for (UdfTreeEntity UdfTreeEntity : udfTreeList) {
      // 判断当前节点是否为根节点，此处注意：若parentId类型是String，则要采用equals()方法判断。
      if (-1 == UdfTreeEntity.getParent()) {
        // 是，添加
        rootudfTreeList.add(UdfTreeEntity);
      }
    }
    return rootudfTreeList;
  }

  /**
   * 根据每一个顶级节点（根节点）进行构建树形结构
   *
   * @return 构建整棵树
   */
  public List<UdfTreeEntity> buildTree() {
    // UdfTreeEntitys：保存一个顶级节点所构建出来的完整树形
    List<UdfTreeEntity> UdfTreeEntitys = new ArrayList<UdfTreeEntity>();
    // getRootNode()：获取所有的根节点
    for (UdfTreeEntity treeRootNode : getRootNode()) {
      // 将顶级节点进行构建子树
      treeRootNode = buildChildTree(treeRootNode);
      // 完成一个顶级节点所构建的树形，增加进来
      UdfTreeEntitys.add(treeRootNode);
    }
    return UdfTreeEntitys;
  }

  /**
   * 递归-----构建子树形结构
   *
   * @param udfTreeEntity 根节点（顶级节点）
   * @return 整棵树
   */
  public UdfTreeEntity buildChildTree(UdfTreeEntity udfTreeEntity) {
    List<UdfTreeEntity> childTree = new ArrayList<UdfTreeEntity>();
    // udfTreeList：所有节点集合（所有数据）
    for (UdfTreeEntity UdfTreeEntity : udfTreeList) {
      // 判断当前节点的父节点ID是否等于根节点的ID，即当前节点为其下的子节点
      if (UdfTreeEntity.getParent().equals(udfTreeEntity.getId())) {
        // 再递归进行判断当前节点的情况，调用自身方法
        childTree.add(buildChildTree(UdfTreeEntity));
      }
    }
    // for循环结束，即节点下没有任何节点，树形构建结束，设置树结果
    udfTreeEntity.setChildrenList(childTree);
    return udfTreeEntity;
  }
}
