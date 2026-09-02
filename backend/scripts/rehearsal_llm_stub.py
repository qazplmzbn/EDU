"""Local OpenAI-compatible fixture for rehearsal-only F004 verification."""
from http.server import BaseHTTPRequestHandler, HTTPServer
import json
import os
import re

MODE = os.getenv("REHEARSAL_LLM_STUB_MODE", "pass").lower()
CAREER_CALLS = 0

GENERATION = {"items": [
    {"itemCode":"stub-teach","itemType":"concept_explanation","purpose":"TEACHING_RESOURCE","visibility":"VISIBLE","title":"Stub explanation","content":{"text":"grounded"},"knowledgePointIds":[7],"evidenceIds":[1]},
    {"itemCode":"stub-visible","itemType":"single_choice","purpose":"LEARNING_PRACTICE","visibility":"VISIBLE","title":"Stub visible","content":{"prompt":"choose"},"knowledgePointIds":[7],"evidenceIds":[1],"questionDifficulty":0.5,"cognitiveLevel":"UNDERSTAND","gradingKey":{"standardAnswer":"A"}},
    {"itemCode":"stub-hidden","itemType":"short_answer","purpose":"KNOWLEDGE_ASSESSMENT","visibility":"HIDDEN_UNTIL_ASSESSMENT","title":"Stub hidden","content":{"prompt":"apply"},"knowledgePointIds":[7],"evidenceIds":[1],"questionDifficulty":0.6,"cognitiveLevel":"APPLY","gradingKey":{"standardAnswer":"B"}}
]}

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        payload = json.loads(self.rfile.read(length) or b"{}")
        prompt = payload.get("messages", [{"content": ""}])[-1].get("content", "")
        if "occupationSkillId" in prompt and "Frozen input=" in prompt:
            # F007 fixture: echo the server-frozen universe and optionally force
            # a two-round disagreement resolved by the third majority round.
            global CAREER_CALLS
            CAREER_CALLS += 1
            frozen = json.loads(prompt.split("Frozen input=", 1)[1])
            level = "INTERMEDIATE"
            if MODE == "career-majority" and CAREER_CALLS % 3 == 2:
                level = "BEGINNER"
            result = {"levels": [{"occupationSkillId": item["occupationSkillId"], "level": level, "reason": "rehearsal fixture"} for item in frozen["skills"]]}
        elif "expertRole=" in prompt:
            role_match = re.search(r'expertRole=([A-Z_]+)', prompt)
            role = role_match.group(1) if role_match else 'UNKNOWN'
            result = {"expertRole": role, "result": "REVISE" if MODE == "revise" else "PASS", "issues": ["fixture"] if MODE == "revise" else []}
        else:
            # The workflow serializes the ResourceUnit in the generator prompt.
            # Use its first knowledge point when present so the fixture never
            # violates the Blueprint scope it is meant to validate.
            match = re.search(r'\\?"knowledgePointIds\\?"\s*:\s*\\?\[\s*(\d+)', prompt)
            knowledge_id = int(match.group(1)) if match else 8
            blueprint_match = re.search(r'\\?"blueprintCode\\?"\s*:\s*\\?"(bp_[a-z0-9]+)', prompt)
            blueprint_code = blueprint_match.group(1) if blueprint_match else "fixture"
            result = json.loads(json.dumps(GENERATION))
            for item in result["items"]:
                item["knowledgePointIds"] = [knowledge_id]
            result["items"][0]["content"] = {"text": f"grounded explanation {knowledge_id} {blueprint_code}"}
            result["items"][1]["content"] = {"prompt": f"visible practice {knowledge_id} {blueprint_code}"}
            result["items"][2]["content"] = {"prompt": f"hidden assessment {knowledge_id} {blueprint_code}"}
        body = json.dumps({"choices": [{"message": {"content": json.dumps(result, ensure_ascii=False)}}]}).encode()
        self.send_response(200); self.send_header("Content-Type", "application/json"); self.send_header("Content-Length", str(len(body))); self.end_headers(); self.wfile.write(body)
    def log_message(self, *_): pass

if __name__ == "__main__": HTTPServer(("127.0.0.1", 19090), Handler).serve_forever()
