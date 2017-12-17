package mc.compiler;

import lombok.AllArgsConstructor;
import lombok.Getter;
import mc.process_models.ProcessModel;

import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
/**
 *  This class is a return type for the calculations regarding diagram creation and operation output.
 */
public class CompilationObject {
    /**
     * processMap stores the model name to ProcessModel. I.e "a" -> diagram
     */
    private Map<String, ProcessModel> processMap;
    /**
     *  operationResults stores the outcome of operation {}, user label transition system (LTS) code.
     */
    private List<OperationResult> operationResults;

    private List<OperationResult> equationResults;
}