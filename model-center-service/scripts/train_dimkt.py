"""Train a versioned DIMKT model from validated resource_interaction JSONL."""
import argparse, hashlib, json
from pathlib import Path
import torch
from torch import nn
from src.models.dimkt import DIMKTModel

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--input",required=True,help="JSONL: knowledgePointId, questionDifficulty, scoreNormalized")
    p.add_argument("--knowledge-index",required=True,help="JSON array of stable knowledge point codes/ids")
    p.add_argument("--output-dir",required=True)
    p.add_argument("--model-version",required=True)
    p.add_argument("--knowledge-index-version",required=True)
    p.add_argument("--hidden-size",type=int,default=128)
    p.add_argument("--epochs",type=int,default=10)
    args=p.parse_args()
    knowledge=[str(x) for x in json.loads(Path(args.knowledge_index).read_text(encoding="utf-8"))]
    index={code:i for i,code in enumerate(knowledge)}
    rows=[json.loads(line) for line in Path(args.input).read_text(encoding="utf-8").splitlines() if line.strip()]
    if not rows: raise SystemExit("No validated resource_interaction training rows")
    model=DIMKTModel(len(knowledge),args.hidden_size)
    optimizer=torch.optim.Adam(model.parameters(),lr=1e-3)
    loss_fn=nn.BCELoss()
    for _ in range(args.epochs):
        hidden=torch.zeros((1,args.hidden_size))
        for row in rows:
            code=str(row["knowledgePointId"])
            if code not in index: continue
            kp=torch.tensor([index[code]],dtype=torch.long)
            difficulty=torch.tensor([[float(row["questionDifficulty"])]],dtype=torch.float32)
            score=torch.tensor([[float(row["scoreNormalized"])]],dtype=torch.float32)
            head,hidden=model(kp,difficulty,score,hidden.detach())
            target=torch.tensor([float(row["scoreNormalized"])],dtype=torch.float32)
            loss=loss_fn(head[0,index[code]].reshape(1),target)
            optimizer.zero_grad();loss.backward();optimizer.step()
    output=Path(args.output_dir)/args.model_version;output.mkdir(parents=True,exist_ok=True)
    weights=output/"weights.pt";torch.save(model.state_dict(),weights)
    digest=hashlib.sha256(weights.read_bytes()).hexdigest()
    manifest={"modelType":"DIMKT","modelVersion":args.model_version,"knowledgeIndexVersion":args.knowledge_index_version,"knowledgeIndex":knowledge,"hiddenSize":args.hidden_size,"weightsFile":weights.name,"weightsSha256":digest,"trainingRows":len(rows)}
    (output/"manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
    print(json.dumps({"modelVersion":args.model_version,"weightsSha256":digest,"rows":len(rows)}))

if __name__=="__main__":main()
