/*
 * Copyright (C) 2017 The ORT Project Authors (see <https://github.com/oss-review-toolkit/ort/blob/main/NOTICE>)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * License-Filename: LICENSE
 */

import React from 'react';

import {
    ArrowDownOutlined,
    ArrowUpOutlined,
    FileAddOutlined,
    FileExcelOutlined
} from '@ant-design/icons';
import {
    Button,
    Collapse,
    Drawer,
    Input,
    message,
    Row,
    Tree
} from 'antd';
import PropTypes from 'prop-types';
import scrollIntoView from 'scroll-into-view-if-needed';

import { connect } from 'react-redux';

import {
    getOrtResult,
    getTreeView,
    getTreeViewShouldComponentUpdate
} from '../reducers/selectors';
import store from '../store';

import PackageDetails from './PackageDetails';
import PackageFindingsTable from './PackageFindingsTable';
import PackageLicenses from './PackageLicenses';
import PackagePaths from './PackagePaths';
import PathExcludesTable from './PathExcludesTable';
import ScopeExcludesTable from './ScopeExcludesTable';

const { Search } = Input;

class TreeView extends React.Component {
    componentDidMount() {
        this.scrollIntoView();
    }

    shouldComponentUpdate() {
        const { shouldComponentUpdate } = this.props;
        return shouldComponentUpdate;
    }

    componentDidUpdate() {
        this.scrollIntoView();
    }

    onChangeSearch = (e) => {
        e.stopPropagation();

        const { value } = e.target;
        const searchValue = value.trim();
        const { webAppOrtResult } = this.props;
        const searchPackageTreeNodeKeys = (searchValue === '')
            ? []
            : webAppOrtResult.packages
                .reduce((acc, webAppPackage) => {
                    if (webAppPackage.id.indexOf(searchValue) > -1) {
                        const treeNodeKeys = webAppOrtResult.getTreeNodeParentKeysByIndex(webAppPackage.packageIndex);
                        if (treeNodeKeys) {
                            acc.push(treeNodeKeys);
                        }
                    }

                    return acc;
                }, []);

        const expandedKeys = Array.from(searchPackageTreeNodeKeys
            .reduce(
                (acc, treeNodeKeys) => new Set([...acc, ...treeNodeKeys.parentKeys]),
                new Set()
            ));
        const matchedKeys = Array.from(searchPackageTreeNodeKeys
            .reduce(
                (acc, treeNodeKeys) => new Set([...acc, ...treeNodeKeys.keys]),
                new Set()
            ));

        if (matchedKeys.length === 0 && searchValue !== '') {
            message.info(`No search results found for '${searchValue}'`);
        }

        store.dispatch({
            type: 'TREE::SEARCH',
            payload: {
                expandedKeys,
                matchedKeys,
                searchValue
            }
        });
    };

    onExpandTreeNode = (expandedKeys) => {
        store.dispatch({ type: 'TREE::NODE_EXPAND', expandedKeys });
    };

    onSelectTreeNode = (selectedKeys, e) => {
        const { node: { key } } = e;
        const { webAppOrtResult } = this.props;

        if (key) {
            const webAppTreeNode = webAppOrtResult.getTreeNodeByKey(key);

            if (webAppTreeNode && !webAppTreeNode.isScope) {
                store.dispatch({
                    type: 'TREE::NODE_SELECT',
                    payload: {
                        selectedWebAppTreeNode: webAppTreeNode,
                        selectedKeys: [key]
                    }
                });
            }
        }
    }

    onClickPreviousSearchMatch = (e) => {
        e.stopPropagation();
        this.scrollIntoView();
        store.dispatch({ type: 'TREE::SEARCH_PREVIOUS_MATCH' });
    }

    onClickNextSearchMatch = (e) => {
        e.stopPropagation();
        this.scrollIntoView();
        store.dispatch({ type: 'TREE::SEARCH_NEXT_MATCH' });
    }

    onCloseDrawer = (e) => {
        e.stopPropagation();
        store.dispatch({ type: 'TREE::DRAWER_CLOSE' });
    }

    scrollIntoView = () => {
        const {
            treeView: {
                selectedKeys
            }
        } = this.props;

        if (selectedKeys.length === 0) {
            return;
        }

        const selectedElemClassName = `ort-tree-node-${selectedKeys[0]}`;
        const selectedElem = document.querySelector(`.${selectedElemClassName}`);

        if (selectedElem) {
            scrollIntoView(selectedElem, {
                scrollMode: 'if-needed',
                boundary: document.querySelector('.ort-tree-wrapper')
            });
        }
    }

    render() {
        const {
            treeView: {
                autoExpandParent,
                expandedKeys,
                matchedKeys,
                searchIndex,
                selectedWebAppTreeNode,
                selectedKeys,
                showDrawer
            },
            webAppOrtResult
        } = this.props;
        const { dependencyTrees } = webAppOrtResult;

        return (
            <div className="ort-tree">
                <div className="ort-tree-search">
                    <Search
                        placeholder="Input search text and press Enter"
                        onPressEnter={this.onChangeSearch}
                    />
                    {
                        matchedKeys.length
                            ? (
                            <Row
                                type="flex"
                                align="middle"
                                className="ort-tree-navigation"
                            >
                                <Button onClick={this.onClickNextSearchMatch}>
                                    <ArrowDownOutlined />
                                </Button>
                                <Button onClick={this.onClickPreviousSearchMatch}>
                                    <ArrowUpOutlined />
                                </Button>
                                <span className="ort-tree-navigation-counter">
                                    {searchIndex + 1}
                                    {' '}
                                    /
                                    {matchedKeys.length}
                                </span>
                            </Row>
                                )
                            : null
                    }
                </div>
                <div className="ort-tree-wrapper">
                    <Tree
                        autoExpandParent={autoExpandParent}
                        expandedKeys={expandedKeys}
                        showLine={true}
                        selectedKeys={selectedKeys}
                        treeData={dependencyTrees}
                        onExpand={this.onExpandTreeNode}
                        onSelect={this.onSelectTreeNode}
                    />
                </div>
                {
                    !!selectedWebAppTreeNode && !selectedWebAppTreeNode.isScope && <div className="ort-tree-drawer">
                            <Drawer
                                placement="right"
                                closable={true}
                                open={showDrawer}
                                width="65%"
                                title={
                                    (() => {
                                        if (webAppOrtResult.hasExcludes()) {
                                            if (selectedWebAppTreeNode.isExcluded) {
                                                return (
                                                    <span>
                                                        <FileExcelOutlined
                                                            className="ort-excluded"
                                                        />
                                                        {' '}
                                                        {selectedWebAppTreeNode.packageName}
                                                    </span>
                                                );
                                            }

                                            return (
                                                <span>
                                                    <FileAddOutlined />
                                                    {' '}
                                                    {selectedWebAppTreeNode.packageName}
                                                </span>
                                            );
                                        }

                                        return (
                                            <span>
                                                {selectedWebAppTreeNode.packageName}
                                            </span>
                                        );
                                    })()
                                }
                                onClose={this.onCloseDrawer}
                            >
                                <Collapse
                                    className="ort-package-collapse"
                                    bordered={false}
                                    defaultActiveKey={[0, 1]}
                                    items={(() => {
                                        const collapseItems = [
                                            {
                                                label: 'Details',
                                                key: 'package-details',
                                                children: (
                                                    <PackageDetails webAppPackage={selectedWebAppTreeNode.package} />
                                                )
                                            }
                                        ];

                                        if (selectedWebAppTreeNode.package.hasLicenses()) {
                                            collapseItems.push({
                                                label: 'Licenses',
                                                key: 'package-licenses',
                                                children: (
                                                    <PackageLicenses webAppPackage={selectedWebAppTreeNode.package} />
                                                )
                                            });
                                        }

                                        if (selectedWebAppTreeNode.package.hasPaths()) {
                                            collapseItems.push({
                                                label: 'Paths',
                                                key: 'package-paths',
                                                children: (
                                                    <PackagePaths
                                                        paths={[selectedWebAppTreeNode.webAppPath]}
                                                    />
                                                )
                                            });
                                        }

                                        if (selectedWebAppTreeNode.package.hasFindings()) {
                                            collapseItems.push({
                                                label: 'Scan Results',
                                                key: 'package-scan-results',
                                                children: (
                                                    <PackageFindingsTable
                                                        webAppPackage={selectedWebAppTreeNode.package}
                                                    />
                                                )
                                            });
                                        }

                                        if (selectedWebAppTreeNode.package.hasPathExcludes()) {
                                            collapseItems.push({
                                                label: 'Path Excludes',
                                                key: 'package-path-excludes',
                                                children: (
                                                    <PathExcludesTable
                                                        excludes={selectedWebAppTreeNode.package.pathExcludes}
                                                    />
                                                )
                                            });
                                        }

                                        if (selectedWebAppTreeNode.package.hasScopeExcludes()) {
                                            collapseItems.push({
                                                label: 'Scope Excludes',
                                                key: 'package-scope-excludes',
                                                children: (
                                                    <ScopeExcludesTable
                                                        excludes={selectedWebAppTreeNode.package.scopeExcludes}
                                                    />
                                                )
                                            });
                                        }

                                        return collapseItems;
                                    })()}
                                />
                            </Drawer>
                        </div>
                }
            </div>
        );
    }
}

TreeView.propTypes = {
    shouldComponentUpdate: PropTypes.bool.isRequired,
    treeView: PropTypes.object.isRequired,
    webAppOrtResult: PropTypes.object.isRequired
};

const mapStateToProps = (state) => ({
    shouldComponentUpdate: getTreeViewShouldComponentUpdate(state),
    treeView: getTreeView(state),
    webAppOrtResult: getOrtResult(state)
});

export default connect(
    mapStateToProps,
    () => ({})
)(TreeView);
