package net.thucydides.core.requirements;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.thucydides.core.reports.json.gson.CollectionAdapter;
import net.thucydides.core.reports.json.gson.OptionalTypeAdapter;
import net.thucydides.core.requirements.model.Requirement;

import java.io.*;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.SortedMap;

public class RequirementPersister {
    private final File outputDirectory;
    private final String rootDirectory;
    Gson gson;

    public RequirementPersister(File outputDirectory, String rootDirectory) {
        this.outputDirectory = outputDirectory;
        this.rootDirectory = rootDirectory;
        this.gson = new GsonBuilder()
                .registerTypeAdapterFactory(OptionalTypeAdapter.FACTORY)
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();

    }

    public SortedMap<String, Requirement> read() throws IOException{
        SortedMap<String, Requirement> map = new ChildrenFirstOrderedMap();
        File jsonFile = new File(outputDirectory, rootDirectory + ".json");
        if(!jsonFile.exists()) {
            return map;
        }

        SortedMap<String, Requirement> storedRequirementsMap;
        Type requirementsMapType = new TypeToken<SortedMap<String, Requirement>>(){}.getType();
        try(FileReader reader = new FileReader(jsonFile)) {
            storedRequirementsMap = gson.fromJson(reader, requirementsMapType);
        }
        map.putAll(storedRequirementsMap);

        //reset the parents
        for (Map.Entry<String, Requirement> entry : storedRequirementsMap.entrySet()) {
            String key = entry.getKey();
            if (key.contains(".")) {
                String parent = key.substring(0, key.lastIndexOf("."));
                Requirement child = entry.getValue();
                updateParentChildren(map, parent, child);
            }
        }
        return map;
    }

    private void updateParentChildren(SortedMap<String, Requirement> map, String parent, Requirement entry) {
        Requirement parentRequirement = map.get(parent);
        Requirement updatedParentRequirement = parentRequirement.withChild(entry);
        map.remove(parent);
        map.put(parent, updatedParentRequirement);
    }

    public void write(SortedMap<String, Requirement> map) throws IOException {
        try( Writer writer = new FileWriter(new File(outputDirectory, rootDirectory + ".json"))) {
            gson.toJson(map, writer);
            writer.flush();
            writer.close();
        }
    }
}