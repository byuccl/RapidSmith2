/*
 * Copyright (c) 2016 Brigham Young University
 *
 * This file is part of the BYU RapidSmith Tools.
 *
 * BYU RapidSmith Tools is free software: you may redistribute it
 * and/or modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * BYU RapidSmith Tools is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * A copy of the GNU General Public License is included with the BYU
 * RapidSmith Tools. It can be found at doc/LICENSE.GPL3.TXT. You may
 * also get a copy of the license at <http://www.gnu.org/licenses/>.
 */
package edu.byu.ece.rapidSmith.bitstreamTools.bitstream.test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XilinxPartgenDescription {

    public XilinxPartgenDescription(List<String> partNames, List<List<String>> validPackages, List<List<String>> validSpeedGrades) {
        _partNames = new ArrayList<>(partNames);
        
        _validPackages = new LinkedHashMap<>();
        _validSpeedGrades = new LinkedHashMap<>();
        int i = 0;
        for (String partName : _partNames) {
            _validPackages.put(partName, new ArrayList<>(validPackages.get(i)));
            _validSpeedGrades.put(partName, new ArrayList<>(validSpeedGrades.get(i)));
            i++;
        }
    }

    public List<String> getPartNames() {
        return new ArrayList<>(_partNames);
    }
    
    public List<String> getValidPackagesForPart(String partName) {
        List<String> result = null;
        List<String> packages = _validPackages.get(partName);
        if (packages != null) {
            result = new ArrayList<>(packages);
        }
        return result;
    }
    
    public List<String> getValidSpeedGradesForPart(String partName) {
        List<String> result = null;
        List<String> speedGrades = _validSpeedGrades.get(partName);
        if (speedGrades != null) {
            result = new ArrayList<>(speedGrades);
        }
        return result;
    }
    
    protected List<String> _partNames;
    protected Map<String, List<String>> _validPackages;
    protected Map<String, List<String>> _validSpeedGrades;
}
