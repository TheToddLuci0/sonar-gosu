/*
 * Copyright 2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codenarc.rule.size

import org.gmetrics.source.SourceCode
import org.codehaus.groovy.ast.ModuleNode

/**
 * Adapter that adapts from a GMetrics SourceCode object to a CodeNarc SourceCode object.
 *
 * @author Chris Mair
 * @version $Revision: 327 $ - $Date: 2010-04-26 06:22:09 +0400 (Пн, 26 апр 2010) $
 */
class GMetricsSourceCodeAdapter implements SourceCode {

    private codeNarcSourceCode

    GMetricsSourceCodeAdapter(org.codenarc.source.SourceCode sourceCode) {
        assert sourceCode
        codeNarcSourceCode = sourceCode
    }

    String getName() {
        return codeNarcSourceCode.name
    }

    String getPath() {
        return codeNarcSourceCode.path
    }

    String getText() {
        return codeNarcSourceCode.text
    }

    List getLines() {
        return codeNarcSourceCode.lines
    }

    String line(int lineNumber) {
        return codeNarcSourceCode.line(lineNumber)
    }

    ModuleNode getAst() {
        return codeNarcSourceCode.ast
    }

    int getLineNumberForCharacterIndex(int charIndex) {
        return codeNarcSourceCode.getLineNumberForCharacterIndex(charIndex)
    }
}
