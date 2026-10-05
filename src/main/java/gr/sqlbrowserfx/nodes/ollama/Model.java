package gr.sqlbrowserfx.nodes.ollama;

import gr.sqlbrowserfx.utils.mapper.DTO;

@DTO
public class Model {

    public class Details {

        private String parameterSize;

        public Details() {
        }

        public Details(String parameterSize) {
            this.parameterSize = parameterSize;
        }

        public String getParameterSize() {
            return parameterSize;
        }

        public void setParameterSize(String parameterSize) {
            this.parameterSize = parameterSize;
        }
    }

    private String name;
    private String model;
    private Details details;

    public Model() {
    }

    public Model(String name, String model, Details details) {
        this.name = name;
        this.model = model;
        this.details = details;
    }

    public String getName() {
        return name;
    }

    public String getModel() {
        return model;
    }

    public Details getDetails() {
        return details;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public void setDetails(Details details) {
        this.details = details;
    }
}
